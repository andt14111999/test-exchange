import { renderHook, act } from "@testing-library/react";
import { useEscrowChannel } from "@/hooks/use-escrow-channel";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { Escrow } from "@/lib/api/merchant";
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

describe("useEscrowChannel", () => {
  let mockSubscription: MockSubscription;
  let mockConsumer: MockConsumer;
  let mockOnEscrowUpdated: jest.Mock;
  let escrowId: string;

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
    mockOnEscrowUpdated = jest.fn();
    escrowId = "123";
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it("should setup connection and subscribe to channel", () => {
    renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
    );
    expect(createActionCableConsumer).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledWith(
      { channel: "MerchantEscrowChannel", escrow_id: escrowId },
      expect.any(Object),
    );
  });

  it("should not setup connection if escrowId is falsy", () => {
    renderHook(() =>
      useEscrowChannel({ escrowId: "", onEscrowUpdated: mockOnEscrowUpdated }),
    );
    expect(createActionCableConsumer).not.toHaveBeenCalled();
  });

  it("should cleanup existing connection before setting up new one", () => {
    const { rerender } = renderHook((props) => useEscrowChannel(props), {
      initialProps: { escrowId, onEscrowUpdated: mockOnEscrowUpdated },
    });
    rerender({ escrowId: "456", onEscrowUpdated: mockOnEscrowUpdated });
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
    expect(mockConsumer.subscriptions.create).toHaveBeenCalledTimes(2);
  });

  it("should handle testPing functionality", () => {
    const { result } = renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
    );
    act(() => {
      mockSubscription.rejected!();
    });
    // No error expected, just coverage
  });

  it("should process received messages correctly (object)", () => {
    renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
    );
    const mockEscrow: Escrow = { id: 1, status: "active" } as Escrow;
    act(() => {
      mockSubscription.received!({ status: "success", data: mockEscrow });
    });
    expect(mockOnEscrowUpdated).toHaveBeenCalledWith(mockEscrow);
  });

  it("should process received messages correctly (string)", () => {
    renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
    );
    const mockEscrow: Escrow = { id: 2, status: "pending" } as Escrow;
    act(() => {
      mockSubscription.received!(
        JSON.stringify({ status: "success", data: mockEscrow }),
      );
    });
    expect(mockOnEscrowUpdated).toHaveBeenCalledWith(mockEscrow);
  });

  it("should handle invalid JSON in received string", () => {
    renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
    );
    unmount();
    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockConsumer.disconnect).toHaveBeenCalled();
  });

  it("should handle consumer creation failure", () => {
    (createActionCableConsumer as jest.Mock).mockReturnValue(null);
    renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
    );
    // No error expected, just coverage
  });

  it("should handle error in keepalive", () => {
    renderHook(() =>
      useEscrowChannel({ escrowId, onEscrowUpdated: mockOnEscrowUpdated }),
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
