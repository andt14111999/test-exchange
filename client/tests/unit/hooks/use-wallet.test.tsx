import { renderHook, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useWallet } from "@/hooks/use-wallet";
import { useUserStore } from "@/lib/store/user-store";
import { useBalanceStore } from "@/lib/store/balance-store";
import { apiClient } from "@/lib/api/client";
import type { BalanceData } from "@/lib/api/balance";

// Mock the modules
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));
jest.mock("@/lib/store/balance-store", () => ({
  useBalanceStore: jest.fn(),
}));
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("useWallet", () => {
  let queryClient: QueryClient;

  const mockUser = {
    id: "1",
    email: "test@example.com",
    name: "Test User",
    role: "user",
  };

  const mockBalanceData = {
    coin_accounts: [
      {
        coin_currency: "BTC",
        balance: 1.5,
        frozen_balance: 0.5,
      },
    ],
    fiat_accounts: [
      {
        currency: "USD",
        balance: 1000,
        frozen_balance: 100,
      },
    ],
  };

  const mockWalletResponse = {
    status: "success",
    data: {
      coin_accounts: [
        {
          id: 1,
          user_id: 1,
          coin_currency: "BTC",
          balance: 2.0,
          frozen_balance: 0.5,
          created_at: "2024-03-20T00:00:00Z",
          updated_at: "2024-03-20T00:00:00Z",
        },
      ],
      fiat_accounts: [
        {
          id: 1,
          user_id: 1,
          currency: "USD",
          balance: 2000,
          frozen_balance: 200,
          created_at: "2024-03-20T00:00:00Z",
          updated_at: "2024-03-20T00:00:00Z",
        },
      ],
    },
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          gcTime: 0,
          staleTime: 0,
          refetchOnMount: false,
          refetchOnWindowFocus: false,
          refetchOnReconnect: false,
        },
      },
    });

    // Reset all mocks
    jest.clearAllMocks();

    // Mock useUserStore
    (useUserStore as unknown as jest.Mock).mockImplementation(() => ({
      user: mockUser,
    }));

    // Mock useBalanceStore
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) => {
      const state = {
        balanceData: null,
        balanceUpdated: false,
      };
      return selector(state);
    });

    // Mock apiClient
    (apiClient.get as jest.Mock).mockResolvedValue({
      status: 200,
      data: mockWalletResponse,
    });
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  it("should fetch wallet data when user is available", async () => {
    const { result } = renderHook(() => useWallet(), { wrapper });

    // Initial state should be loading
    expect(result.current.isLoading).toBe(true);

    // Wait for the query to settle
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100));
    });

    // Log the current state for debugging
    console.log("Current query state:", {
      data: result.current.data,
      isLoading: result.current.isLoading,
      error: result.current.error,
    });

    // Ensure the loading state is false
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();

    // Check if the data is correct
    expect(result.current.data).toEqual(mockWalletResponse.data);
  });

  it("should not fetch wallet data when user is not available", () => {
    // Mock user as null
    (useUserStore as unknown as jest.Mock).mockImplementation(() => ({
      user: null,
    }));

    // Mock balance data as null
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) => {
      const state = {
        balanceData: null,
        balanceUpdated: false,
      };
      return selector(state);
    });

    const { result } = renderHook(() => useWallet(), { wrapper });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.data).toBeUndefined();
  });

  it("should use balance store data when query data is not available", async () => {
    (apiClient.get as jest.Mock).mockRejectedValue(new Error("API Error"));

    // Mock balance data
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) => {
      const state = {
        balanceData: mockBalanceData,
        balanceUpdated: false,
      };
      return selector(state);
    });

    const { result } = renderHook(() => useWallet(), { wrapper });

    // Wait for the query to settle
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100));
    });

    expect(result.current.data).toEqual({
      coin_accounts: [
        {
          id: 0,
          user_id: 1,
          coin_currency: "BTC",
          balance: 1.5,
          frozen_balance: 0.5,
          created_at: expect.any(String),
          updated_at: expect.any(String),
        },
      ],
      fiat_accounts: [
        {
          id: 0,
          user_id: 1,
          currency: "USD",
          balance: 1000,
          frozen_balance: 100,
          created_at: expect.any(String),
          updated_at: expect.any(String),
        },
      ],
    });
  });

  it("should trigger rerender when balance is updated", async () => {
    let balanceUpdatedCallback: (state: {
      balanceData: BalanceData | null;
      balanceUpdated: boolean;
    }) => boolean;
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) => {
      balanceUpdatedCallback = selector;
      return false;
    });

    const { result, rerender } = renderHook(() => useWallet(), { wrapper });

    // Initial render
    expect(result.current.isLoading).toBe(true);

    // Wait for the query to settle
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100));
    });

    // Simulate balance update
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) => {
      if (selector === balanceUpdatedCallback) {
        return true;
      }
      return mockBalanceData;
    });

    // Rerender the hook
    rerender();

    // Wait for the effect to run
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100));
    });

    // The query should be refetched
    expect(apiClient.get).toHaveBeenCalledTimes(2);
  });

  it("should handle undefined balance data", () => {
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) => {
      const state = {
        balanceData: undefined,
        balanceUpdated: false,
      };
      return selector(state);
    });

    const { result } = renderHook(() => useWallet(), { wrapper });

    expect(result.current.data).toBeUndefined();
  });
});
