import { renderHook, act } from "@testing-library/react";
import { useAmmPositionChannel } from "@/hooks/use-positions-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { AmmPosition } from "@/lib/api/positions";

// Mock the action cable consumer
jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
});

describe("useAmmPositionChannel", () => {
  const mockConsumer = {
    subscriptions: {
      create: jest.fn(),
    },
    disconnect: jest.fn(),
  };

  const mockSubscription = {
    unsubscribe: jest.fn(),
    perform: jest.fn(),
  };

  const mockPosition: AmmPosition = {
    id: 1,
    identifier: "pos_123",
    pool_pair: "btc_usdt",
    tick_lower_index: -100,
    tick_upper_index: 100,
    status: "open",
    error_message: null,
    liquidity: "1000000",
    amount0: "1",
    amount1: "20000",
    amount0_initial: "1",
    amount1_initial: "20000",
    slippage: "0.005",
    fee_growth_inside0_last: "100",
    fee_growth_inside1_last: "200",
    tokens_owed0: "0.1",
    tokens_owed1: "2000",
    fee_collected0: "0.05",
    fee_collected1: "1000",
    created_at: 1647734400,
    updated_at: 1647734400,
  };

  const mockOnPositionUpdate = jest.fn();
  const mockOnPositionCreated = jest.fn();
  const mockOnPositionClosed = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockLocalStorage.getItem.mockReturnValue("mock-token");
    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
    mockConsumer.subscriptions.create.mockReturnValue(mockSubscription);
  });

  it("should not connect when userId is 0", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 0,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    expect(createActionCableConsumer).not.toHaveBeenCalled();
  });

  it("should not connect when no token in localStorage", () => {
    mockLocalStorage.getItem.mockReturnValue(null);

    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalled();
  });

  it("should create subscription with correct parameters", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
        onPositionCreated: mockOnPositionCreated,
        onPositionClosed: mockOnPositionClosed,
      }),
    );

    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      {
        channel: "AmmPositionChannel",
        user_id: 123,
      },
      expect.objectContaining({
        connected: expect.any(Function),
        disconnected: expect.any(Function),
        rejected: expect.any(Function),
        received: expect.any(Function),
      }),
    );
  });

  it("should handle position update message", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler({
        status: "success",
        data: mockPosition,
        action: "updated",
      });
    });

    expect(mockOnPositionUpdate).toHaveBeenCalledWith(mockPosition);
  });

  it("should handle position created message", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
        onPositionCreated: mockOnPositionCreated,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler({
        status: "success",
        data: mockPosition,
        action: "created",
      });
    });

    expect(mockOnPositionCreated).toHaveBeenCalledWith(mockPosition);
  });

  it("should handle position closed message", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
        onPositionClosed: mockOnPositionClosed,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler({
        status: "success",
        data: mockPosition,
        action: "closed",
      });
    });

    expect(mockOnPositionClosed).toHaveBeenCalledWith(mockPosition.id);
  });

  it("should handle position closed by ID only", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
        onPositionClosed: mockOnPositionClosed,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler({
        status: "success",
        action: "closed",
        position_id: 456,
      });
    });

    expect(mockOnPositionClosed).toHaveBeenCalledWith(456);
  });

  it("should handle multiple positions update", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    const multiplePositions = [mockPosition, { ...mockPosition, id: 2 }];

    act(() => {
      receivedHandler({
        status: "success",
        data: multiplePositions,
        action: "updated",
      });
    });

    expect(mockOnPositionUpdate).toHaveBeenCalledTimes(2);
    expect(mockOnPositionUpdate).toHaveBeenCalledWith(mockPosition);
    expect(mockOnPositionUpdate).toHaveBeenCalledWith({
      ...mockPosition,
      id: 2,
    });
  });

  it("should handle string response", () => {
    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler(
        JSON.stringify({
          status: "success",
          data: mockPosition,
          action: "updated",
        }),
      );
    });

    expect(mockOnPositionUpdate).toHaveBeenCalledWith(mockPosition);
  });

  it("should handle system messages", () => {
    const consoleSpy = jest.spyOn(console, "log").mockImplementation();

    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler({
        status: "success",
        message: "System message",
      });
    });

    expect(mockOnPositionUpdate).not.toHaveBeenCalled();
    expect(consoleSpy).toHaveBeenCalledWith(
      "AmmPositionChannel system message:",
      "System message",
    );

    consoleSpy.mockRestore();
  });

  it("should handle connection events", () => {
    const consoleSpy = jest.spyOn(console, "log").mockImplementation();

    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const { connected, disconnected, rejected } = subscriptionCall[1];

    act(() => {
      connected();
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      "⚡ Connected to AmmPositionChannel for user 123",
    );

    act(() => {
      disconnected();
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      "💤 Disconnected from AmmPositionChannel for user 123",
    );

    act(() => {
      rejected();
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      "❌ AmmPositionChannel connection rejected for user 123",
    );

    consoleSpy.mockRestore();
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    unmount();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle errors gracefully", () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();

    renderHook(() =>
      useAmmPositionChannel({
        userId: 123,
        onPositionUpdate: mockOnPositionUpdate,
      }),
    );

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedHandler = subscriptionCall[1].received;

    act(() => {
      receivedHandler("invalid json");
    });

    expect(mockOnPositionUpdate).not.toHaveBeenCalled();

    consoleSpy.mockRestore();
  });
});
