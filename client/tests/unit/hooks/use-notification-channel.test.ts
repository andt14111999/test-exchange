import { renderHook, act } from "@testing-library/react";
import { useNotificationChannel } from "@/hooks/use-notification-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";

// Mock the ActionCable consumer
jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

interface MockSubscription {
  unsubscribe: jest.Mock;
  perform: jest.Mock;
  connected: () => void;
  disconnected: () => void;
  rejected: () => void;
  received: (data: unknown) => void;
}

type MockConsumerSubscriptions = {
  create: jest.Mock;
};

interface MockConsumer {
  disconnect: jest.Mock;
  subscriptions: MockConsumerSubscriptions;
}

describe("useNotificationChannel", () => {
  const mockUserId = 123;
  const mockOnNotificationReceived = jest.fn();
  let mockSubscription: MockSubscription;
  let mockConsumer: MockConsumer;

  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();

    // Reset mocks
    mockOnNotificationReceived.mockReset();

    // Setup mock subscription
    mockSubscription = {
      unsubscribe: jest.fn(),
      perform: jest.fn(),
      connected: jest.fn(),
      disconnected: jest.fn(),
      rejected: jest.fn(),
      received: jest.fn(),
    };

    // Setup mock consumer
    mockConsumer = {
      disconnect: jest.fn(),
      subscriptions: {
        create: jest.fn().mockImplementation((_, handlers) => {
          // Store handlers for testing
          Object.assign(mockSubscription, handlers);
          return mockSubscription;
        }),
      },
    };

    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it("should setup connection and subscription when mounted", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      {
        channel: "NotificationChannel",
      },
      expect.any(Object),
    );
  });

  it("should not setup connection when userId is falsy", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: 0,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    expect(createActionCableConsumer).not.toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).not.toHaveBeenCalled();
  });

  it("should cleanup existing connection before setting up new one", () => {
    const { rerender } = renderHook((props) => useNotificationChannel(props), {
      initialProps: {
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      },
    });

    // Rerender with different userId
    rerender({
      userId: mockUserId + 1,
      onNotificationReceived: mockOnNotificationReceived,
    });

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledTimes(2);
  });

  it("should handle testPing functionality", () => {
    const { result } = renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    act(() => {
      result.current.testPing();
      jest.advanceTimersByTime(100);
    });

    expect(mockSubscription.perform).toHaveBeenCalledWith(
      "ping",
      expect.objectContaining({
        message: "Hello from client",
        timestamp: expect.any(Number),
        client_id: expect.any(String),
      }),
    );
  });

  it("should start keepalive on connection and stop on disconnection", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    // Simulate connection
    act(() => {
      mockSubscription.connected();
    });

    // Fast-forward 30 seconds
    act(() => {
      jest.advanceTimersByTime(30000);
    });

    expect(mockSubscription.perform).toHaveBeenCalledWith(
      "keepalive",
      expect.objectContaining({
        timestamp: expect.any(Number),
      }),
    );

    // Simulate disconnection
    act(() => {
      mockSubscription.disconnected();
    });

    // Clear previous calls
    mockSubscription.perform.mockClear();

    // Fast-forward another 30 seconds
    act(() => {
      jest.advanceTimersByTime(30000);
    });

    // Should not perform keepalive after disconnection
    expect(mockSubscription.perform).not.toHaveBeenCalled();
  });

  it("should handle received notifications correctly", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    // Test success with data
    act(() => {
      mockSubscription.received({
        status: "success",
        data: {
          id: 1,
          title: "Test",
          content: "Test content",
          type: "notification",
          read: false,
          created_at: "2024-03-20T00:00:00Z",
        },
      });
    });

    expect(mockOnNotificationReceived).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 1,
        title: "Test",
      }),
    );

    // Test success with message (should not trigger notification)
    mockOnNotificationReceived.mockClear();
    act(() => {
      mockSubscription.received({
        status: "success",
        message: "Test message",
      });
    });

    expect(mockOnNotificationReceived).not.toHaveBeenCalled();

    // Test with string response
    act(() => {
      mockSubscription.received(
        JSON.stringify({
          title: "String Test",
          content: "Test content",
          id: 2,
        }),
      );
    });

    expect(mockOnNotificationReceived).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 2,
        title: "String Test",
      }),
    );

    // Test with invalid string response
    mockOnNotificationReceived.mockClear();
    act(() => {
      mockSubscription.received("invalid json");
    });

    expect(mockOnNotificationReceived).not.toHaveBeenCalled();

    // Test with minimal notification data
    act(() => {
      mockSubscription.received({
        title: "Minimal Test",
      });
    });

    expect(mockOnNotificationReceived).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "Minimal Test",
        content: "",
        type: "notification",
        read: false,
      }),
    );
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    unmount();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle consumer creation failure", () => {
    (createActionCableConsumer as jest.Mock).mockReturnValue(null);

    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    expect(mockConsumer.subscriptions.create).not.toHaveBeenCalled();
  });

  it("should handle rejection", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    act(() => {
      mockSubscription.rejected();
    });

    // Fast-forward 30 seconds
    act(() => {
      jest.advanceTimersByTime(30000);
    });

    // Should not perform keepalive after rejection
    expect(mockSubscription.perform).not.toHaveBeenCalled();
  });

  it("should attempt reconnection after disconnection", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    act(() => {
      mockSubscription.disconnected();
    });

    // Fast-forward 3 seconds (reconnection delay)
    act(() => {
      jest.advanceTimersByTime(3000);
    });

    // Should attempt to create a new subscription
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledTimes(2);
  });

  it("should handle errors in processResponse", () => {
    renderHook(() =>
      useNotificationChannel({
        userId: mockUserId,
        onNotificationReceived: mockOnNotificationReceived,
      }),
    );

    // Mock onNotificationReceived to throw an error
    mockOnNotificationReceived.mockImplementation(() => {
      throw new Error("Test error");
    });

    act(() => {
      mockSubscription.received({
        status: "success",
        data: {
          id: 1,
          title: "Test",
          content: "Test content",
          type: "notification",
          read: false,
          created_at: "2024-03-20T00:00:00Z",
        },
      });
    });

    // The test should pass without throwing an error
    expect(mockOnNotificationReceived).toHaveBeenCalled();
  });
});
