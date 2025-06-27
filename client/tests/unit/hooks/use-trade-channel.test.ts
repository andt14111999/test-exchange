import { renderHook, act } from "@testing-library/react";
import { useTradeChannel } from "@/hooks/use-trade-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { ApiTrade } from "@/lib/api/trades";

// Mock the action cable consumer
jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

interface ResponseWithData {
  status?: string;
  data?: ApiTrade;
  message?: string;
  [key: string]: unknown;
}

describe("useTradeChannel", () => {
  const mockTradeId = "trade-123";
  const mockOnTradeUpdated = jest.fn();
  const mockSubscription = {
    perform: jest.fn(),
    unsubscribe: jest.fn(),
  };
  const mockConsumer = {
    subscriptions: {
      create: jest.fn(),
    },
    disconnect: jest.fn(),
  };

  const mockTrade: ApiTrade = {
    id: mockTradeId,
    ref: "ref-123",
    status: "active",
    taker_side: "buy",
    coin_amount: "100",
    coin_currency: "USDT",
    fiat_amount: "1000000",
    fiat_currency: "VND",
    price: "10000",
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
    seller: {
      id: "seller-123",
      email: "seller@example.com",
      display_name: "Seller Name",
    },
    buyer: {
      id: "buyer-123",
      email: "buyer@example.com",
      display_name: "Buyer Name",
    },
    payment_method: "bank_transfer",
    payment_details: {
      bank_name: "Test Bank",
      bank_account_name: "Test Account",
      bank_account_number: "1234567890",
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
    mockConsumer.subscriptions.create.mockReturnValue(mockSubscription);
  });

  it("should create a subscription when tradeId is provided", () => {
    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      {
        channel: "TradeChannel",
        trade_id: mockTradeId,
      },
      expect.any(Object),
    );
  });

  it("should not create a subscription when tradeId is empty", () => {
    renderHook(() =>
      useTradeChannel({
        tradeId: "",
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    expect(createActionCableConsumer).not.toHaveBeenCalled();
  });

  it("should handle successful trade updates", () => {
    let receivedCallback:
      | ((response: ResponseWithData | string) => void)
      | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      receivedCallback = callbacks.received;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (receivedCallback) {
        receivedCallback({
          status: "success",
          data: mockTrade,
        });
      }
    });

    expect(mockOnTradeUpdated).toHaveBeenCalledWith(mockTrade);
  });

  it("should handle string responses", () => {
    let receivedCallback:
      | ((response: ResponseWithData | string) => void)
      | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      receivedCallback = callbacks.received;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (receivedCallback) {
        receivedCallback(
          JSON.stringify({
            status: "success",
            data: mockTrade,
          }),
        );
      }
    });

    expect(mockOnTradeUpdated).toHaveBeenCalledWith(mockTrade);
  });

  it("should handle invalid JSON string responses", () => {
    let receivedCallback:
      | ((response: ResponseWithData | string) => void)
      | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      receivedCallback = callbacks.received;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (receivedCallback) {
        receivedCallback("invalid json");
      }
    });

    expect(mockOnTradeUpdated).not.toHaveBeenCalled();
  });

  it("should handle responses without success status", () => {
    let receivedCallback:
      | ((response: ResponseWithData | string) => void)
      | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      receivedCallback = callbacks.received;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (receivedCallback) {
        receivedCallback({
          status: "error",
          message: "Something went wrong",
        });
      }
    });

    expect(mockOnTradeUpdated).not.toHaveBeenCalled();
  });

  it("should handle responses without data", () => {
    let receivedCallback:
      | ((response: ResponseWithData | string) => void)
      | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      receivedCallback = callbacks.received;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (receivedCallback) {
        receivedCallback({
          status: "success",
        });
      }
    });

    expect(mockOnTradeUpdated).not.toHaveBeenCalled();
  });

  it("should handle testPing", () => {
    jest.useFakeTimers();
    const { result } = renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      result.current.testPing();
    });

    act(() => {
      jest.advanceTimersByTime(100);
    });

    expect(mockSubscription.perform).toHaveBeenCalledWith("ping", {
      message: "Hello from client",
      timestamp: expect.any(Number),
      client_id: expect.any(String),
    });

    jest.useRealTimers();
  });

  it("should handle testPing error", () => {
    jest.useFakeTimers();
    mockSubscription.perform.mockImplementation(() => {
      throw new Error("Test error");
    });

    const { result } = renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      result.current.testPing();
    });

    act(() => {
      jest.advanceTimersByTime(100);
    });

    // Should not throw error
    expect(mockSubscription.perform).toHaveBeenCalled();

    jest.useRealTimers();
  });

  it("should handle keepalive", () => {
    jest.useFakeTimers();
    let connectedCallback: (() => void) | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      connectedCallback = callbacks.connected;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (connectedCallback) {
        connectedCallback();
      }
    });

    act(() => {
      jest.advanceTimersByTime(30000);
    });

    expect(mockSubscription.perform).toHaveBeenCalledWith("keepalive", {
      timestamp: expect.any(Number),
    });

    jest.useRealTimers();
  });

  it("should stop keepalive when subscription is null", () => {
    jest.useFakeTimers();
    let connectedCallback: (() => void) | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      connectedCallback = callbacks.connected;
      return mockSubscription;
    });

    const { unmount } = renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (connectedCallback) {
        connectedCallback();
      }
    });

    unmount();

    act(() => {
      jest.advanceTimersByTime(30000);
    });

    expect(mockSubscription.perform).not.toHaveBeenCalled();

    jest.useRealTimers();
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    unmount();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle reconnection on disconnect", () => {
    jest.useFakeTimers();
    let disconnectedCallback: (() => void) | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      disconnectedCallback = callbacks.disconnected;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (disconnectedCallback) {
        disconnectedCallback();
      }
    });

    act(() => {
      jest.advanceTimersByTime(3000);
    });

    expect(createActionCableConsumer).toHaveBeenCalledTimes(2);

    jest.useRealTimers();
  });

  it("should handle rejected connection", () => {
    let rejectedCallback: (() => void) | null = null;

    mockConsumer.subscriptions.create.mockImplementation((_, callbacks) => {
      rejectedCallback = callbacks.rejected;
      return mockSubscription;
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    act(() => {
      if (rejectedCallback) {
        rejectedCallback();
      }
    });

    expect(mockSubscription.unsubscribe).not.toHaveBeenCalled();
  });

  it("should handle error in setupConnection", () => {
    mockConsumer.subscriptions.create.mockImplementation(() => {
      throw new Error("Test error");
    });

    renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    // Should not throw error
    expect(mockConsumer.subscriptions.create).toHaveBeenCalled();
  });

  it("should handle error in cleanup", () => {
    mockSubscription.unsubscribe.mockImplementation(() => {
      throw new Error("Test error");
    });

    const { unmount } = renderHook(() =>
      useTradeChannel({
        tradeId: mockTradeId,
        onTradeUpdated: mockOnTradeUpdated,
      }),
    );

    unmount();

    // Should not throw error
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
  });
});
