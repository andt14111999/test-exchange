import { apiClient } from "@/lib/api/client";
import { fetchCoinTransactions } from "@/lib/api/coin-transactions";
import type { CoinTransactionsResponse } from "@/types/coin-transactions";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Coin Transactions API", () => {
  const mockCoinCurrency = "BTC";
  const mockTransactionsResponse: CoinTransactionsResponse = {
    status: "success",
    data: {
      deposits: [
        {
          id: 1,
          amount: 1.23456789,
          coin_currency: "BTC",
          status: "completed",
          hash: "0x123456789",
          address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
          created_at: "2024-03-20T12:00:00Z",
          updated_at: "2024-03-20T12:00:00Z",
        },
      ],
      withdrawals: [
        {
          id: 2,
          amount: 0.5,
          coin_currency: "BTC",
          status: "completed",
          hash: "0x987654321",
          address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
          created_at: "2024-03-20T12:00:00Z",
          updated_at: "2024-03-20T12:00:00Z",
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

  describe("fetchCoinTransactions", () => {
    it("should fetch coin transactions successfully with default pagination", async () => {
      const mockResponse = {
        data: mockTransactionsResponse,
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await fetchCoinTransactions({
        coin_currency: mockCoinCurrency,
      });

      expect(apiClient.get).toHaveBeenCalledWith("/coin_transactions", {
        params: {
          coin_currency: mockCoinCurrency,
          page: 1,
          per_page: 20,
        },
      });
      expect(result).toEqual(mockTransactionsResponse);
    });

    it("should fetch coin transactions successfully with custom pagination", async () => {
      const mockResponse = {
        data: mockTransactionsResponse,
      };
      const customPage = 2;
      const customPerPage = 50;

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await fetchCoinTransactions({
        coin_currency: mockCoinCurrency,
        page: customPage,
        per_page: customPerPage,
      });

      expect(apiClient.get).toHaveBeenCalledWith("/coin_transactions", {
        params: {
          coin_currency: mockCoinCurrency,
          page: customPage,
          per_page: customPerPage,
        },
      });
      expect(result).toEqual(mockTransactionsResponse);
    });

    it("should handle error when fetching coin transactions fails", async () => {
      const mockError = new Error("Failed to fetch coin transactions");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        fetchCoinTransactions({ coin_currency: mockCoinCurrency }),
      ).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
