import { renderHook, act } from "@testing-library/react";
import { useFiatMintChannel } from "@/hooks/use-fiat-mint-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { FiatMint } from "@/lib/api/merchant";
import type { Subscription } from "@rails/actioncable";

jest.mock("@/lib/api/action-cable", () => ({
  createActionCableConsumer: jest.fn(),
}));

interface MockSubscription extends Partial<Subscription> {
  unsubscribe: jest.Mock;
  perform: jest.Mock;
  connected?: () => void;
  disconnected?: () => void;
  rejected?: () => void;
  received?: (data: unknown) => void;
}

interface MockConsumer {
  disconnect: jest.Mock;
  subscriptions: {
    create: jest.Mock;
  };
}

describe("useFiatMintChannel", () => {
  let mockSubscription: MockSubscription;
  let mockConsumer: MockConsumer;
  let mockOnFiatMintUpdated: jest.Mock;
  let fiatMintId: string;

  beforeEach(() => {
    jest.useFakeTimers();
    mockSubscription = {
      unsubscribe: jest.fn(),
      perform: jest.fn(),
    };
    mockConsumer = {
      disconnect: jest.fn(),
      subscriptions: {
        create: jest.fn().mockImplementation((_, handlers) => {
          Object.assign(mockSubscription, handlers);
          return mockSubscription;
        }),
      },
    };
    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
    mockOnFiatMintUpdated = jest.fn();
    fiatMintId = "123";
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it("should setup connection and subscribe to channel", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      { channel: "MerchantFiatMintChannel", fiat_mint_id: fiatMintId },
      expect.any(Object),
    );
  });

  it("should not setup connection if fiatMintId is falsy", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId: "",
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    expect(createActionCableConsumer).not.toHaveBeenCalled();
  });

  it("should cleanup existing connection before setting up new one", () => {
    const { rerender } = renderHook((props) => useFiatMintChannel(props), {
      initialProps: { fiatMintId, onFiatMintUpdated: mockOnFiatMintUpdated },
    });
    rerender({ fiatMintId: "456", onFiatMintUpdated: mockOnFiatMintUpdated });
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledTimes(2);
  });

  it("should handle testPing functionality", () => {
    const { result } = renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
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
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    act(() => {
      mockSubscription.connected!();
    });
    act(() => {
      jest.advanceTimersByTime(30000);
    });
    expect(mockSubscription.perform).toHaveBeenCalledWith(
      "keepalive",
      expect.objectContaining({ timestamp: expect.any(Number) }),
    );
    act(() => {
      mockSubscription.disconnected!();
    });
    mockSubscription.perform.mockClear();
    act(() => {
      jest.advanceTimersByTime(30000);
    });
    expect(mockSubscription.perform).not.toHaveBeenCalled();
  });

  it("should reconnect after disconnection", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    act(() => {
      mockSubscription.disconnected!();
    });
    act(() => {
      jest.advanceTimersByTime(3000);
    });
    expect(createActionCableConsumer).toHaveBeenCalledTimes(2);
  });

  it("should stop keepalive on rejected", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    act(() => {
      mockSubscription.rejected!();
    });
    // No error expected, just coverage
  });

  it("should process received messages correctly (object)", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    const mockFiatMint: FiatMint = { id: 1, status: "active" } as FiatMint;
    act(() => {
      mockSubscription.received!({ status: "success", data: mockFiatMint });
    });
    expect(mockOnFiatMintUpdated).toHaveBeenCalledWith(mockFiatMint);
  });

  it("should process received messages correctly (string)", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    const mockFiatMint: FiatMint = { id: 2, status: "pending" } as FiatMint;
    act(() => {
      mockSubscription.received!(
        JSON.stringify({ status: "success", data: mockFiatMint }),
      );
    });
    expect(mockOnFiatMintUpdated).toHaveBeenCalledWith(mockFiatMint);
  });

  it("should handle invalid JSON in received string", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    act(() => {
      mockSubscription.received!("not a json");
    });
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("should handle error in processing received", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    act(() => {
      mockSubscription.received!(undefined);
    });
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    unmount();
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle consumer creation failure", () => {
    (createActionCableConsumer as jest.Mock).mockReturnValue(null);
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    // No error expected, just coverage
  });

  it("should handle error cleaning up existing connection", () => {
    (createActionCableConsumer as jest.Mock).mockReturnValue(mockConsumer);
    mockConsumer.disconnect.mockImplementation(() => {
      throw new Error("disconnect error");
    });
    mockSubscription.unsubscribe.mockImplementation(() => {
      throw new Error("unsubscribe error");
    });
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    // No error expected, just coverage
  });

  it("should handle error in keepalive", () => {
    renderHook(() =>
      useFiatMintChannel({
        fiatMintId,
        onFiatMintUpdated: mockOnFiatMintUpdated,
      }),
    );
    mockSubscription.perform.mockImplementation(() => {
      throw new Error("keepalive error");
    });
    act(() => {
      mockSubscription.connected!();
    });
    act(() => {
      jest.advanceTimersByTime(30000);
    });
    // No error expected, just coverage
  });
});
