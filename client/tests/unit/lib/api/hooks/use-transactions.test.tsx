import React from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useTransactions } from "@/lib/api/hooks/use-transactions";
import { apiClient } from "@/lib/api/client";
import type { Transaction } from "@/types/transaction";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;

// Sample mock data
const mockTransaction: Transaction = {
  id: "tx-1",
  type: "deposit",
  status: "completed",
  amount: 1.0,
  coin_currency: "BTC",
  hash: "0x123",
  created_at: "2024-03-20T00:00:00Z",
  updated_at: "2024-03-20T00:00:00Z",
};

const mockTransactionsResponse = {
  data: [mockTransaction],
  total_pages: 1,
  current_page: 1,
  total_count: 1,
};

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        refetchOnMount: false,
        refetchOnReconnect: false,
        refetchOnWindowFocus: false,
        networkMode: "always",
      },
    },
  });

const createWrapper = () => {
  const queryClient = createTestQueryClient();
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "TestQueryClientProvider";
  return Wrapper;
};

describe("useTransactions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.clearAllTimers();
  });

  it("should fetch transactions successfully", async () => {
    mockApiClient.get.mockResolvedValueOnce({ data: mockTransactionsResponse });

    const { result } = renderHook(() => useTransactions(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);

    await waitFor(
      () => {
        expect(result.current.isSuccess).toBe(true);
      },
      { timeout: 3000 },
    );

    expect(mockApiClient.get).toHaveBeenCalledWith(
      "/transactions?page=1&per_page=10",
    );
    expect(result.current.data).toEqual(mockTransactionsResponse);
  });

  it("should handle pagination", async () => {
    mockApiClient.get.mockResolvedValueOnce({
      data: {
        ...mockTransactionsResponse,
        current_page: 2,
      },
    });

    const { result } = renderHook(() => useTransactions({ page: 2 }), {
      wrapper: createWrapper(),
    });

    await waitFor(
      () => {
        expect(result.current.isSuccess).toBe(true);
      },
      { timeout: 3000 },
    );

    expect(mockApiClient.get).toHaveBeenCalledWith(
      "/transactions?page=2&per_page=10",
    );
  });

  it("should retry on failure", async () => {
    const error = new Error("Failed to fetch transactions");
    mockApiClient.get
      .mockRejectedValueOnce(error)
      .mockRejectedValueOnce(error)
      .mockResolvedValueOnce({ data: mockTransactionsResponse });

    const queryClient = createTestQueryClient();
    queryClient.setDefaultOptions({
      queries: {
        ...queryClient.getDefaultOptions().queries,
        retry: 2,
        retryDelay: 100,
      },
    });

    const customWrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    customWrapper.displayName = "RetryTestWrapper";

    const { result } = renderHook(() => useTransactions(), {
      wrapper: customWrapper,
    });

    // Initial loading state
    expect(result.current.isLoading).toBe(true);
    expect(result.current.isError).toBe(false);

    // Wait for success after retries
    await waitFor(
      () => {
        expect(result.current.isSuccess).toBe(true);
        expect(result.current.data).toEqual(mockTransactionsResponse);
      },
      { timeout: 5000 },
    );

    // Verify retry behavior
    expect(mockApiClient.get).toHaveBeenCalledTimes(3);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.error).toBeNull();
  });
});
