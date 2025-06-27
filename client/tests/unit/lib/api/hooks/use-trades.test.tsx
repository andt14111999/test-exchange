import React from "react";
import { renderHook, waitFor, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useTrades,
  useTrade,
  useCreateTrade,
  useMarkTradePaid,
  useDisputeTrade,
  useReleaseTrade,
  useCancelTrade,
} from "@/lib/api/hooks/use-trades";
import {
  getTrades,
  getTrade,
  createTrade,
  markTradePaid,
  releaseTrade,
  disputeTrade,
  cancelTrade,
} from "@/lib/api/trades";
import type { ApiTrade } from "@/lib/api/trades";

// Mock the trades API module
jest.mock("@/lib/api/trades");

const mockGetTrades = getTrades as jest.Mock;
const mockGetTrade = getTrade as jest.Mock;
const mockCreateTrade = createTrade as jest.Mock;
const mockMarkTradePaid = markTradePaid as jest.Mock;
const mockReleaseTrade = releaseTrade as jest.Mock;
const mockDisputeTrade = disputeTrade as jest.Mock;
const mockCancelTrade = cancelTrade as jest.Mock;

// Sample mock data
const mockTrade: ApiTrade = {
  id: "trade-1",
  ref: "REF123",
  coin_currency: "BTC",
  fiat_currency: "USD",
  fiat_amount: "30000",
  coin_amount: "1",
  price: "30000",
  status: "pending",
  taker_side: "buy",
  payment_method: "bank",
  payment_details: {
    bank_name: "Test Bank",
    bank_account_number: "1234567890",
    bank_account_name: "John Doe",
    bank_id: "1",
  },
  amount_after_fee: "0.99",
  coin_trading_fee: "0.01",
  fee_ratio: "0.01",
  created_at: "2024-03-20T00:00:00Z",
  updated_at: "2024-03-20T00:00:00Z",
  dispute_reason: undefined,
  dispute_resolution: undefined,
  buyer: {
    id: "user-1",
    email: "buyer@example.com",
    display_name: "buyer",
  },
  seller: {
    id: "user-2",
    email: "seller@example.com",
    display_name: "seller",
  },
};

const mockTrades: ApiTrade[] = [
  mockTrade,
  {
    ...mockTrade,
    id: "trade-2",
    coin_amount: "2",
    fiat_amount: "60000",
  },
];

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
        retryDelay: 1,
        networkMode: "always",
      },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, queryClient] as const;
};

describe("Trade Hooks", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("useTrades", () => {
    it("should fetch trades list successfully", async () => {
      mockGetTrades.mockResolvedValueOnce(mockTrades);

      const { result } = renderHook(() => useTrades(), {
        wrapper: createWrapper()[0],
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockTrades);
      expect(mockGetTrades).toHaveBeenCalled();
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch trades");
      mockGetTrades.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTrades(), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isError).toBe(true);
      expect(result.current.error).toBeDefined();
    });

    it("should fetch trades with params", async () => {
      mockGetTrades.mockResolvedValueOnce(mockTrades);
      const params = { id: "trade-1", status: "pending" };

      const { result } = renderHook(() => useTrades(params), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(mockGetTrades).toHaveBeenCalledWith(params);
      expect(result.current.data).toEqual(mockTrades);
    });
  });

  describe("useTrade", () => {
    it("should fetch a single trade successfully", async () => {
      mockGetTrade.mockResolvedValueOnce(mockTrade);

      const { result } = renderHook(() => useTrade("trade-1"), {
        wrapper: createWrapper()[0],
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockTrade);
      expect(mockGetTrade).toHaveBeenCalledWith("trade-1");
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch trade");
      mockGetTrade.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTrade("trade-1"), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isError).toBe(true);
      expect(result.current.error).toBeDefined();
    });
  });

  describe("useCreateTrade", () => {
    it("should create trade successfully", async () => {
      mockCreateTrade.mockResolvedValueOnce(mockTrade);

      const { result } = renderHook(() => useCreateTrade(), {
        wrapper: createWrapper()[0],
      });

      const createParams = {
        offer_id: "offer-1",
        coin_amount: 1,
      };

      await act(async () => {
        const promise = result.current.mutateAsync(createParams);
        await waitFor(() => {
          expect(result.current.isSuccess).toBe(true);
        });
        await promise;
      });

      expect(mockCreateTrade).toHaveBeenCalledWith(createParams);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to create trade");
      mockCreateTrade.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useCreateTrade(), {
        wrapper: createWrapper()[0],
      });

      const createParams = {
        offer_id: "offer-1",
        coin_amount: 1,
      };

      await act(async () => {
        try {
          await result.current.mutateAsync(createParams);
        } catch (e) {
          expect(e).toBeDefined();
        }
        await waitFor(() => {
          expect(result.current.isError).toBe(true);
        });
      });

      expect(result.current.error).toBeDefined();
    });
  });

  describe("useMarkTradePaid", () => {
    it("should mark trade as paid successfully", async () => {
      mockMarkTradePaid.mockResolvedValueOnce(mockTrade);

      const { result } = renderHook(() => useMarkTradePaid(), {
        wrapper: createWrapper()[0],
      });

      const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
      const params = {
        id: "trade-1",
        payment_receipt_details: {
          file,
          description: "Payment receipt",
        },
      };

      await act(async () => {
        const promise = result.current.mutateAsync(params);
        await waitFor(() => {
          expect(result.current.isSuccess).toBe(true);
        });
        await promise;
      });

      expect(mockMarkTradePaid).toHaveBeenCalledWith(
        params.id,
        params.payment_receipt_details,
      );
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to mark trade as paid");
      mockMarkTradePaid.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useMarkTradePaid(), {
        wrapper: createWrapper()[0],
      });

      const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
      const params = {
        id: "trade-1",
        payment_receipt_details: {
          file,
          description: "Payment receipt",
        },
      };

      await act(async () => {
        try {
          await result.current.mutateAsync(params);
        } catch (e) {
          expect(e).toBeDefined();
        }
        await waitFor(() => {
          expect(result.current.isError).toBe(true);
        });
      });

      expect(result.current.error).toBeDefined();
    });
  });

  describe("useDisputeTrade", () => {
    it("should dispute trade successfully", async () => {
      mockDisputeTrade.mockResolvedValueOnce(mockTrade);

      const { result } = renderHook(() => useDisputeTrade(), {
        wrapper: createWrapper()[0],
      });

      const params = {
        id: "trade-1",
        params: {
          dispute_reason: "Payment not received",
        },
      };

      await act(async () => {
        const promise = result.current.mutateAsync(params);
        await waitFor(() => {
          expect(result.current.isSuccess).toBe(true);
        });
        await promise;
      });

      expect(mockDisputeTrade).toHaveBeenCalledWith(params.id, params.params);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to dispute trade");
      mockDisputeTrade.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useDisputeTrade(), {
        wrapper: createWrapper()[0],
      });

      const params = {
        id: "trade-1",
        params: {
          dispute_reason: "Payment not received",
        },
      };

      await act(async () => {
        try {
          await result.current.mutateAsync(params);
        } catch (e) {
          expect(e).toBeDefined();
        }
        await waitFor(() => {
          expect(result.current.isError).toBe(true);
        });
      });

      expect(result.current.error).toBeDefined();
    });
  });

  describe("useReleaseTrade", () => {
    it("should release trade successfully", async () => {
      mockReleaseTrade.mockResolvedValueOnce(mockTrade);

      const { result } = renderHook(() => useReleaseTrade(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        const promise = result.current.mutateAsync("trade-1");
        await waitFor(() => {
          expect(result.current.isSuccess).toBe(true);
        });
        await promise;
      });

      expect(mockReleaseTrade).toHaveBeenCalledWith("trade-1");
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to release trade");
      mockReleaseTrade.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useReleaseTrade(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        try {
          await result.current.mutateAsync("trade-1");
        } catch (e) {
          expect(e).toBeDefined();
        }
        await waitFor(() => {
          expect(result.current.isError).toBe(true);
        });
      });

      expect(result.current.error).toBeDefined();
    });
  });

  describe("useCancelTrade", () => {
    it("should cancel trade successfully", async () => {
      mockCancelTrade.mockResolvedValueOnce(mockTrade);

      const { result } = renderHook(() => useCancelTrade(), {
        wrapper: createWrapper()[0],
      });

      const params = {
        id: "trade-1",
        params: {
          cancel_reason: "Changed my mind",
        },
      };

      await act(async () => {
        const promise = result.current.mutateAsync(params);
        await waitFor(() => {
          expect(result.current.isSuccess).toBe(true);
        });
        await promise;
      });

      expect(mockCancelTrade).toHaveBeenCalledWith(params.id, params.params);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to cancel trade");
      mockCancelTrade.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useCancelTrade(), {
        wrapper: createWrapper()[0],
      });

      const params = {
        id: "trade-1",
        params: {
          cancel_reason: "Changed my mind",
        },
      };

      await act(async () => {
        try {
          await result.current.mutateAsync(params);
        } catch (e) {
          expect(e).toBeDefined();
        }
        await waitFor(() => {
          expect(result.current.isError).toBe(true);
        });
      });

      expect(result.current.error).toBeDefined();
    });
  });
});
