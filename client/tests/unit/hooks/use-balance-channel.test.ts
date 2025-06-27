import { renderHook, act } from "@testing-library/react";
import { useBalanceChannel } from "@/hooks/use-balance-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { Subscription } from "@rails/actioncable";
import type { BalanceData } from "@/lib/api/balance";

// Mock the action cable consumer
jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

interface SubscriptionCallbacks {
  connected?: () => void;
  disconnected?: () => void;
  rejected?: () => void;
  received?: (data: unknown) => void;
}

type MockConsumerType = {
  disconnect: jest.Mock;
  subscriptions: {
    create: jest.Mock;
  };
};

describe("useBalanceChannel", () => {
  let mockConsumer: MockConsumerType;
  let mockSubscription: Partial<Subscription>;
  let mockOnBalanceUpdate: jest.Mock<void, [BalanceData]>;
  let subscriptionCallbacks: SubscriptionCallbacks;

  beforeEach(() => {
    jest.useFakeTimers();
    mockOnBalanceUpdate = jest.fn();
    subscriptionCallbacks = {};

    // Setup mock subscription
    mockSubscription = {
      unsubscribe: jest.fn(),
      perform: jest.fn(),
    };

    // Setup mock consumer
    mockConsumer = {
      disconnect: jest.fn(),
      subscriptions: {
        create: jest.fn((channel, callbacks: SubscriptionCallbacks) => {
          Object.assign(subscriptionCallbacks, callbacks);
          return mockSubscription as Subscription;
        }),
      },
    };

    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it("should not setup connection if userId is not provided", () => {
    renderHook(() =>
      useBalanceChannel({
        userId: 0,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    expect(createActionCableConsumer).not.toHaveBeenCalled();
  });

  it("should setup connection with correct channel and user_id", () => {
    const userId = 123;
    renderHook(() =>
      useBalanceChannel({
        userId,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      {
        channel: "BalanceChannel",
      },
      expect.any(Object),
    );
  });

  it("should handle connection lifecycle events", () => {
    const userId = 123;
    renderHook(() =>
      useBalanceChannel({
        userId,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    // Test connected callback
    act(() => {
      subscriptionCallbacks.connected?.();
    });

    // Test disconnected callback
    act(() => {
      subscriptionCallbacks.disconnected?.();
    });

    // Should attempt reconnection after 3 seconds
    act(() => {
      jest.advanceTimersByTime(3000);
    });

    expect(createActionCableConsumer).toHaveBeenCalledTimes(2);

    // Test rejected callback
    act(() => {
      subscriptionCallbacks.rejected?.();
    });
  });

  it("should handle received messages correctly", () => {
    const userId = 123;
    const mockBalanceData = { balance: "100.00", currency: "USD" };

    renderHook(() =>
      useBalanceChannel({
        userId,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    // Test system message
    act(() => {
      subscriptionCallbacks.received?.({
        status: "success",
        message: "System message",
      });
    });
    expect(mockOnBalanceUpdate).not.toHaveBeenCalled();

    // Test balance update
    act(() => {
      subscriptionCallbacks.received?.({
        status: "success",
        data: mockBalanceData,
      });
    });
    expect(mockOnBalanceUpdate).toHaveBeenCalledWith(mockBalanceData);

    // Test string message
    act(() => {
      subscriptionCallbacks.received?.(
        JSON.stringify({
          status: "success",
          data: mockBalanceData,
        }),
      );
    });
    expect(mockOnBalanceUpdate).toHaveBeenCalledWith(mockBalanceData);

    // Test invalid string message
    act(() => {
      subscriptionCallbacks.received?.("invalid json");
    });
    expect(mockOnBalanceUpdate).toHaveBeenCalledTimes(2);

    // Test error in processing
    const consoleSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});
    act(() => {
      subscriptionCallbacks.received?.(undefined);
    });
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("should handle keepalive mechanism", () => {
    const userId = 123;
    renderHook(() =>
      useBalanceChannel({
        userId,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    // Connect to start keepalive
    act(() => {
      subscriptionCallbacks.connected?.();
    });

    // Fast-forward 30 seconds
    act(() => {
      jest.advanceTimersByTime(30000);
    });

    expect(mockSubscription.perform).toHaveBeenCalledWith("keepalive", {
      timestamp: expect.any(Number),
    });

    // Test multiple keepalive intervals
    act(() => {
      jest.advanceTimersByTime(30000);
    });

    expect(mockSubscription.perform).toHaveBeenCalledTimes(2);
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() =>
      useBalanceChannel({
        userId: 123,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    unmount();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle consumer creation failure", () => {
    (createActionCableConsumer as jest.Mock).mockReturnValue(null);

    renderHook(() =>
      useBalanceChannel({
        userId: 123,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    expect(mockConsumer.subscriptions.create).not.toHaveBeenCalled();
  });

  it("should handle subscription creation failure", () => {
    const consoleSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});
    mockConsumer.subscriptions.create.mockImplementation(() => {
      throw new Error("Subscription failed");
    });

    renderHook(() =>
      useBalanceChannel({
        userId: 123,
        onBalanceUpdate: mockOnBalanceUpdate,
      }),
    );

    expect(consoleSpy).toHaveBeenCalledWith(
      "Error creating BalanceChannel subscription:",
      expect.any(Error),
    );
    consoleSpy.mockRestore();
  });

  it("should handle cleanup of existing connection before creating new one", () => {
    const { rerender } = renderHook(
      ({ userId }) =>
        useBalanceChannel({
          userId,
          onBalanceUpdate: mockOnBalanceUpdate,
        }),
      {
        initialProps: { userId: 123 },
      },
    );

    rerender({ userId: 456 });

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
    expect(createActionCableConsumer).toHaveBeenCalledTimes(2);
  });
});
