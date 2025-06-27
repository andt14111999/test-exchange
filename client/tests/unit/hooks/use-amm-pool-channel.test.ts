import { renderHook, act } from "@testing-library/react";
import { useAmmPoolChannel } from "@/hooks/use-amm-pool-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { ApiPool } from "@/lib/api/pools";

// Mock the action cable consumer
jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

interface MockSubscription {
  unsubscribe: jest.Mock;
  perform: jest.Mock;
}

interface MockConsumer {
  disconnect: jest.Mock;
  subscriptions: {
    create: jest.Mock;
  };
}

describe("useAmmPoolChannel", () => {
  let mockSubscription: MockSubscription;
  let mockConsumer: MockConsumer;
  let mockOnPoolUpdate: jest.Mock;
  let mockPerform: jest.Mock;

  beforeEach(() => {
    jest.useFakeTimers();
    mockPerform = jest.fn();
    mockSubscription = {
      unsubscribe: jest.fn(),
      perform: mockPerform,
    };
    mockConsumer = {
      disconnect: jest.fn(),
      subscriptions: {
        create: jest.fn().mockReturnValue(mockSubscription),
      },
    };
    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
    mockOnPoolUpdate = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it("should setup connection and subscribe to channel", () => {
    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      { channel: "AmmPoolChannel" },
      expect.any(Object),
    );
  });

  it("should handle connection lifecycle events", () => {
    let subscriptionCallbacks: {
      connected: () => void;
      disconnected: () => void;
      rejected: () => void;
      received: (data: unknown) => void;
    };

    mockConsumer.subscriptions.create.mockImplementation(
      (channel: unknown, callbacks: typeof subscriptionCallbacks) => {
        subscriptionCallbacks = callbacks;
        return mockSubscription;
      },
    );

    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    // Test connected callback
    act(() => {
      subscriptionCallbacks.connected();
    });
    jest.advanceTimersByTime(30000);
    expect(mockPerform).toHaveBeenCalledWith("keepalive", expect.any(Object));

    // Test disconnected callback
    act(() => {
      subscriptionCallbacks.disconnected();
    });
    jest.advanceTimersByTime(3000);
    expect(createActionCableConsumer).toHaveBeenCalledTimes(2);

    // Test rejected callback
    act(() => {
      subscriptionCallbacks.rejected();
    });
  });

  it("should process received messages correctly", () => {
    let subscriptionCallbacks: {
      connected: () => void;
      disconnected: () => void;
      rejected: () => void;
      received: (data: unknown) => void;
    };

    mockConsumer.subscriptions.create.mockImplementation(
      (channel: unknown, callbacks: typeof subscriptionCallbacks) => {
        subscriptionCallbacks = callbacks;
        return mockSubscription;
      },
    );

    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    const mockPool = {
      id: 1,
      pair: "BTC/USDT",
      token0: "BTC",
      token1: "USDT",
      tick_spacing: 1,
      fee_percentage: "0.003",
      current_tick: 1000,
      sqrt_price: "1000000000000000000",
      price: "50000",
      apr: 10,
      tvl_in_token0: "1000000",
      tvl_in_token1: "50000000000",
      created_at: 1710000000,
      updated_at: 1710000000,
    } satisfies ApiPool;

    // Test system message
    act(() => {
      subscriptionCallbacks.received({
        status: "success",
        message: "System message",
      });
    });
    expect(mockOnPoolUpdate).not.toHaveBeenCalled();

    // Test pool update
    act(() => {
      subscriptionCallbacks.received({
        status: "success",
        data: mockPool,
        action: "updated",
      });
    });
    expect(mockOnPoolUpdate).toHaveBeenCalledWith(mockPool);

    // Test string message
    act(() => {
      subscriptionCallbacks.received(
        JSON.stringify({
          status: "success",
          data: mockPool,
        }),
      );
    });
    expect(mockOnPoolUpdate).toHaveBeenCalledWith(mockPool);

    // Test invalid JSON string
    act(() => {
      subscriptionCallbacks.received("invalid json");
    });
    expect(mockOnPoolUpdate).toHaveBeenCalledTimes(2);

    // Test error in processing
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    act(() => {
      subscriptionCallbacks.received(undefined);
    });
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();

    // Test non-success status
    act(() => {
      subscriptionCallbacks.received({
        status: "error",
        data: mockPool,
      });
    });
    expect(mockOnPoolUpdate).toHaveBeenCalledTimes(2);
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() =>
      useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }),
    );

    unmount();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle consumer creation failure", () => {
    (createActionCableConsumer as jest.Mock).mockReturnValue(null);

    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    expect(mockConsumer.subscriptions.create).not.toHaveBeenCalled();
  });

  it("should handle subscription creation error", () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    mockConsumer.subscriptions.create.mockImplementation(() => {
      throw new Error("Subscription error");
    });

    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    expect(consoleSpy).toHaveBeenCalledWith(
      "Error creating AmmPoolChannel subscription:",
      expect.any(Error),
    );
    consoleSpy.mockRestore();
  });

  it("should handle reconnection when consumer exists", () => {
    // First render with initial consumer
    const { unmount } = renderHook(() =>
      useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }),
    );

    // Unmount to trigger cleanup
    unmount();

    // Verify cleanup was called
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();

    // Reset mocks for second render
    jest.clearAllMocks();

    // Create new mocks for second render
    const newMockSubscription = {
      unsubscribe: jest.fn(),
      perform: jest.fn(),
    };
    const newMockConsumer = {
      disconnect: jest.fn(),
      subscriptions: {
        create: jest.fn().mockReturnValue(newMockSubscription),
      },
    };
    (createActionCableConsumer as jest.Mock).mockReturnValue(newMockConsumer);

    // Second render
    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    // Verify new consumer was created and subscription was made
    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(newMockConsumer.subscriptions.create).toHaveBeenCalledWith(
      { channel: "AmmPoolChannel" },
      expect.any(Object),
    );
  });

  it("should handle keepalive when subscription becomes null", () => {
    let subscriptionCallbacks: {
      connected: () => void;
      disconnected: () => void;
      rejected: () => void;
      received: (data: unknown) => void;
    };

    mockConsumer.subscriptions.create.mockImplementation(
      (channel: unknown, callbacks: typeof subscriptionCallbacks) => {
        subscriptionCallbacks = callbacks;
        return mockSubscription;
      },
    );

    renderHook(() => useAmmPoolChannel({ onPoolUpdate: mockOnPoolUpdate }));

    // Start keepalive
    act(() => {
      subscriptionCallbacks.connected();
    });

    // First keepalive should work
    jest.advanceTimersByTime(30000);
    expect(mockPerform).toHaveBeenCalledWith("keepalive", expect.any(Object));

    // Make subscription null and trigger rejected callback
    act(() => {
      subscriptionCallbacks.rejected();
    });

    // Clear previous calls
    mockPerform.mockClear();

    // Next keepalive interval should not trigger perform
    jest.advanceTimersByTime(30000);
    expect(mockPerform).not.toHaveBeenCalled();
  });
});
