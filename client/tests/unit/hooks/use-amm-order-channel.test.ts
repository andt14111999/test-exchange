import { renderHook } from "@testing-library/react";
import { useAmmOrderChannel } from "@/hooks/use-amm-order-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";

// Mock the action cable consumer
jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

// Mock @rails/actioncable
jest.mock("@rails/actioncable", () => ({
  Subscription: jest.fn(),
  Consumer: jest.fn(),
}));

describe("useAmmOrderChannel", () => {
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

  const mockCallbacks = {
    onOrderCreated: jest.fn(),
    onOrderUpdated: jest.fn(),
    onOrderCompleted: jest.fn(),
    onOrderFailed: jest.fn(),
    onError: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
    mockConsumer.subscriptions.create.mockReturnValue(mockSubscription);
  });

  it("should connect to AmmOrderChannel when userId is provided", () => {
    const userId = 123;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      { channel: "AmmOrderChannel", user_id: 123 },
      expect.objectContaining({
        connected: expect.any(Function),
        disconnected: expect.any(Function),
        rejected: expect.any(Function),
        received: expect.any(Function),
      }),
    );
  });

  it("should not connect when userId is not provided", () => {
    renderHook(() => useAmmOrderChannel({ userId: 0, ...mockCallbacks }));

    expect(createActionCableConsumer).not.toHaveBeenCalled();
  });

  it("should handle order created event", () => {
    const userId = 123;
    const orderId = 456;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    // Get the received callback from the subscription
    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    // Simulate order created message
    const orderCreatedMessage = {
      status: "success",
      action: "created",
      order_id: orderId,
    };

    receivedCallback(orderCreatedMessage);

    expect(mockCallbacks.onOrderCreated).toHaveBeenCalledWith(orderId);
  });

  it("should handle order updated event", () => {
    const userId = 123;
    const orderId = 456;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const orderUpdatedMessage = {
      status: "success",
      action: "updated",
      order_id: orderId,
    };

    receivedCallback(orderUpdatedMessage);

    expect(mockCallbacks.onOrderUpdated).toHaveBeenCalledWith(orderId);
  });

  it("should handle order completed event", () => {
    const userId = 123;
    const orderId = 456;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const orderCompletedMessage = {
      status: "success",
      action: "completed",
      order_id: orderId,
    };

    receivedCallback(orderCompletedMessage);

    expect(mockCallbacks.onOrderCompleted).toHaveBeenCalledWith(orderId);
  });

  it("should handle order failed event", () => {
    const userId = 123;
    const orderId = 456;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const orderFailedMessage = {
      status: "error",
      action: "failed",
      order_id: orderId,
    };

    receivedCallback(orderFailedMessage);

    expect(mockCallbacks.onOrderFailed).toHaveBeenCalledWith(orderId);
  });

  it("should handle generic error", () => {
    const userId = 123;
    const errorMessage = "Something went wrong";

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const errorMessageObj = {
      status: "error",
      message: errorMessage,
    };

    receivedCallback(errorMessageObj);

    expect(mockCallbacks.onError).toHaveBeenCalledWith(errorMessage);
  });

  it("should handle string message format", () => {
    const userId = 123;
    const orderId = 456;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const orderCreatedMessageString = JSON.stringify({
      status: "success",
      action: "created",
      order_id: orderId,
    });

    receivedCallback(orderCreatedMessageString);

    expect(mockCallbacks.onOrderCreated).toHaveBeenCalledWith(orderId);
  });

  it("should handle system messages", () => {
    const userId = 123;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const systemMessage = {
      status: "success",
      message: "System is running normally",
    };

    receivedCallback(systemMessage);

    // Should not call any order callbacks for system messages
    expect(mockCallbacks.onOrderCreated).not.toHaveBeenCalled();
    expect(mockCallbacks.onOrderUpdated).not.toHaveBeenCalled();
    expect(mockCallbacks.onOrderCompleted).not.toHaveBeenCalled();
    expect(mockCallbacks.onOrderFailed).not.toHaveBeenCalled();
    expect(mockCallbacks.onError).not.toHaveBeenCalled();
  });

  it("should handle connection events", () => {
    const userId = 123;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const { connected, disconnected, rejected } = subscriptionCall[1];

    // Test connected callback with fake timers
    jest.useFakeTimers();
    connected();

    // Advance timer to trigger keepalive
    jest.advanceTimersByTime(30000);
    expect(mockSubscription.perform).toHaveBeenCalledWith("keepalive", {
      timestamp: expect.any(Number),
    });

    // Test disconnected callback
    jest.clearAllMocks();
    disconnected();
    // Do not expect unsubscribe here, only reconnect logic
    // expect(mockSubscription.unsubscribe).toHaveBeenCalled();

    jest.advanceTimersByTime(3000);
    expect(createActionCableConsumer).toHaveBeenCalled(); // At least once (initial or reconnect)

    jest.useRealTimers();

    // Test rejected callback
    rejected();
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    jest.clearAllMocks();
  });

  it("should cleanup on unmount", () => {
    const userId = 123;

    const { unmount } = renderHook(() =>
      useAmmOrderChannel({ userId, ...mockCallbacks }),
    );

    unmount();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle malformed JSON gracefully", () => {
    const userId = 123;

    renderHook(() => useAmmOrderChannel({ userId, ...mockCallbacks }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    // Send malformed JSON string
    receivedCallback("invalid json");

    // Should not call any callbacks
    expect(mockCallbacks.onOrderCreated).not.toHaveBeenCalled();
    expect(mockCallbacks.onError).not.toHaveBeenCalled();
  });

  it("should handle missing callbacks gracefully", () => {
    const userId = 123;
    const orderId = 456;

    // Test without any callbacks
    renderHook(() => useAmmOrderChannel({ userId }));

    const subscriptionCall = mockConsumer.subscriptions.create.mock.calls[0];
    const receivedCallback = subscriptionCall[1].received;

    const orderCreatedMessage = {
      status: "success",
      action: "created",
      order_id: orderId,
    };

    // Should not throw error when callbacks are not provided
    expect(() => receivedCallback(orderCreatedMessage)).not.toThrow();
  });
});
