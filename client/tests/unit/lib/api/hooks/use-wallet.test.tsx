import React from "react";
import { renderHook, waitFor, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useWalletBalance, useTransferFunds } from "@/lib/api/hooks/use-wallet";
import { apiClient } from "@/lib/api/client";
import { queryClient } from "@/lib/query/query-client";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

// Mock the query client
jest.mock("@/lib/query/query-client", () => ({
  queryClient: {
    invalidateQueries: jest.fn(),
  },
}));

const mockApiGet = apiClient.get as jest.Mock;
const mockApiPost = apiClient.post as jest.Mock;
const mockInvalidateQueries = queryClient.invalidateQueries as jest.Mock;

// Sample mock data
const mockBalance = {
  balance: "1000.00",
  currency: "USD",
};

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const testQueryClient = new QueryClient({
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
    <QueryClientProvider client={testQueryClient}>
      {children}
    </QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, testQueryClient] as const;
};

describe("Wallet Hooks", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("useWalletBalance", () => {
    it("should fetch wallet balance successfully", async () => {
      mockApiGet.mockResolvedValueOnce(mockBalance);

      const { result } = renderHook(() => useWalletBalance(), {
        wrapper: createWrapper()[0],
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockBalance);
      expect(mockApiGet).toHaveBeenCalledWith("/wallet/balance");
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch balance");
      mockApiGet.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useWalletBalance(), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isError).toBe(true);
      expect(result.current.error).toBeDefined();
    });
  });

  describe("useTransferFunds", () => {
    const transferData = {
      to: "user123",
      amount: "100.00",
      currency: "USD",
    };

    it("should transfer funds successfully", async () => {
      const successResponse = { success: true };
      mockApiPost.mockResolvedValueOnce(successResponse);

      const { result } = renderHook(() => useTransferFunds(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        await result.current.mutateAsync(transferData);
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(mockApiPost).toHaveBeenCalledWith(
        "/wallet/transfer",
        transferData,
      );
      expect(mockInvalidateQueries).toHaveBeenCalledWith({
        queryKey: ["wallet"],
      });
    });

    it("should handle transfer error", async () => {
      const error = new Error("Transfer failed");
      mockApiPost.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTransferFunds(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        result.current.mutate(transferData);
      });

      await waitFor(() => {
        expect(result.current.isError).toBe(true);
      });

      expect(result.current.error).toBeDefined();
    });

    it("should invalidate wallet queries on success", async () => {
      mockApiPost.mockResolvedValueOnce({ success: true });

      const { result } = renderHook(() => useTransferFunds(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        await result.current.mutateAsync(transferData);
      });

      expect(mockInvalidateQueries).toHaveBeenCalledWith({
        queryKey: ["wallet"],
      });
    });
  });
});
