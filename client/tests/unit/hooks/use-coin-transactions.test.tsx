import { renderHook, waitFor } from "@testing-library/react";
import { useCoinTransactions } from "@/hooks/use-coin-transactions";
import { fetchCoinTransactions } from "@/lib/api/coin-transactions";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";

// Mock the API function
jest.mock("@/lib/api/coin-transactions", () => ({
  fetchCoinTransactions: jest.fn(),
}));

const mockFetchCoinTransactions = fetchCoinTransactions as jest.Mock;

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return Wrapper;
};

describe("useCoinTransactions", () => {
  const defaultParams = {
    coin_currency: "btc",
  };

  const mockTransactionsResponse = {
    status: "success",
    data: {
      deposits: [
        {
          id: 1,
          amount: 1.5,
          coin_currency: "btc",
          status: "completed",
          hash: "0x123",
          address: "bc1q...",
          created_at: "2024-03-20T00:00:00Z",
          updated_at: "2024-03-20T00:00:00Z",
        },
      ],
      withdrawals: [
        {
          id: 2,
          amount: 0.5,
          coin_currency: "btc",
          status: "pending",
          hash: "0x456",
          address: "bc1q...",
          created_at: "2024-03-20T00:00:00Z",
          updated_at: "2024-03-20T00:00:00Z",
        },
      ],
      pagination: {
        deposits: {
          current_page: 1,
          total_pages: 1,
          total_count: 1,
          per_page: 20,
        },
        withdrawals: {
          current_page: 1,
          total_pages: 1,
          total_count: 1,
          per_page: 20,
        },
      },
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("fetches coin transactions successfully with default pagination", async () => {
    mockFetchCoinTransactions.mockResolvedValueOnce(mockTransactionsResponse);

    const { result } = renderHook(() => useCoinTransactions(defaultParams), {
      wrapper: createWrapper(),
    });

    // Initial state
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();

    // Wait for the query to resolve
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockTransactionsResponse);
    expect(mockFetchCoinTransactions).toHaveBeenCalledWith({
      coin_currency: "btc",
      page: 1,
      per_page: 20,
    });
  });

  it("fetches coin transactions with custom pagination", async () => {
    mockFetchCoinTransactions.mockResolvedValueOnce(mockTransactionsResponse);

    const customParams = {
      ...defaultParams,
      page: 2,
      per_page: 10,
    };

    const { result } = renderHook(() => useCoinTransactions(customParams), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockTransactionsResponse);
    expect(mockFetchCoinTransactions).toHaveBeenCalledWith({
      coin_currency: "btc",
      page: 2,
      per_page: 10,
    });
  });

  it("handles fetch error correctly", async () => {
    const error = new Error("Failed to fetch transactions");
    mockFetchCoinTransactions.mockRejectedValueOnce(error);

    const { result } = renderHook(() => useCoinTransactions(defaultParams), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBeDefined();
    expect(result.current.data).toBeUndefined();
  });

  it("refetches when query key changes", async () => {
    mockFetchCoinTransactions
      .mockResolvedValueOnce(mockTransactionsResponse)
      .mockResolvedValueOnce({
        ...mockTransactionsResponse,
        data: {
          ...mockTransactionsResponse.data,
          deposits: [],
        },
      });

    const { result, rerender } = renderHook(
      (props) => useCoinTransactions(props),
      {
        wrapper: createWrapper(),
        initialProps: defaultParams,
      },
    );

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // Change the coin currency
    rerender({ ...defaultParams, coin_currency: "eth" });

    await waitFor(() => {
      expect(mockFetchCoinTransactions).toHaveBeenCalledTimes(2);
    });

    expect(mockFetchCoinTransactions).toHaveBeenLastCalledWith({
      coin_currency: "eth",
      page: 1,
      per_page: 20,
    });
  });
});
