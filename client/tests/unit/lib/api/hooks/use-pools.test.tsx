import React from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { usePools, useCreatePool } from "@/lib/api/hooks/use-pools";
import { useAMMStore } from "@/lib/amm/store";
import { apiClient } from "@/lib/api/client";
import { queryClient as defaultQueryClient } from "@/lib/query/query-client";

// Mock dependencies
jest.mock("@/lib/api/client");
jest.mock("@/lib/amm/store", () => ({
  useAMMStore: jest.fn(),
}));
jest.mock("@/lib/amm/constants", () => ({
  POOLS: [
    {
      id: "pool1",
      token0: { name: "token0" },
      token1: { name: "token1" },
    },
  ],
}));

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
        staleTime: Infinity,
      },
    },
  });

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, queryClient] as const;
};

describe("Pool Hooks", () => {
  const mockPoolData = {
    pool1: {
      price: "1.0",
      liquidity: "1000",
    },
  };

  const mockInitializePool = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useAMMStore as unknown as jest.Mock).mockReturnValue(mockInitializePool);
    (apiClient.get as jest.Mock).mockResolvedValue({ data: mockPoolData });
    (apiClient.post as jest.Mock).mockResolvedValue({ data: mockPoolData });
  });

  describe("usePools", () => {
    it("should fetch pools and initialize them in AMM store", async () => {
      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => usePools(), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(true);
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(apiClient.get).toHaveBeenCalledWith("/pools");
      expect(mockInitializePool).toHaveBeenCalledWith(
        "pool1",
        "token0",
        "token1",
        "1.0",
        "1000",
      );
      expect(result.current.data).toEqual(mockPoolData);
    });

    it("should handle error when fetching pools fails", async () => {
      const error = new Error("Failed to fetch pools");
      (apiClient.get as jest.Mock).mockRejectedValue(error);

      const [Wrapper] = createWrapper();

      const { result } = renderHook(() => usePools(), {
        wrapper: Wrapper,
      });

      await waitFor(
        () => {
          expect(result.current.isError).toBe(true);
          expect(result.current.error).toBe(error);
        },
        { timeout: 10000 },
      );
    }, 15000);

    it("should retry 3 times on failure", async () => {
      const error = new Error("Failed to fetch pools");
      (apiClient.get as jest.Mock).mockRejectedValue(error);

      const [Wrapper] = createWrapper();

      const { result } = renderHook(() => usePools(), {
        wrapper: Wrapper,
      });

      await waitFor(
        () => {
          expect(apiClient.get).toHaveBeenCalledTimes(4); // Initial request + 3 retries
        },
        { timeout: 10000 },
      );
    }, 15000);
  });

  describe("useCreatePool", () => {
    it("should create a pool successfully", async () => {
      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useCreatePool(), {
        wrapper: Wrapper,
      });

      const createData = {
        token0: "token0",
        token1: "token1",
        initialPrice: "1.0",
        initialLiquidity: "1000",
      };

      result.current.mutate(createData);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(apiClient.post).toHaveBeenCalledWith("/pools", createData);
      expect(result.current.data).toEqual({ data: mockPoolData });
    });

    it("should handle error when creating a pool fails", async () => {
      const error = new Error("Failed to create pool");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useCreatePool(), {
        wrapper: Wrapper,
      });

      result.current.mutate({
        token0: "token0",
        token1: "token1",
        initialPrice: "1.0",
        initialLiquidity: "1000",
      });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });

    it("should invalidate pools query on successful creation", async () => {
      // Mock the default queryClient's invalidateQueries method
      const invalidateQueriesSpy = jest.spyOn(
        defaultQueryClient,
        "invalidateQueries",
      );

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useCreatePool(), {
        wrapper: Wrapper,
      });

      // Trigger mutation
      result.current.mutate({
        token0: "token0",
        token1: "token1",
        initialPrice: "1.0",
        initialLiquidity: "1000",
      });

      // Wait for mutation to complete
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      // Check if invalidateQueries was called on the default queryClient
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["pools"],
      });
    });
  });
});
