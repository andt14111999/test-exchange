import { apiClient } from "@/lib/api/client";
import { balanceApi } from "@/lib/api/balance";
import { API_ENDPOINTS } from "@/lib/api/config";
import type { BalanceResponse } from "@/lib/api/balance";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Balance API", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getBalance", () => {
    it("should fetch balance data successfully", async () => {
      const mockResponse: { data: BalanceResponse } = {
        data: {
          status: "success",
          data: {
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
          },
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await balanceApi.getBalance();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.wallet.balances);
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching balance fails", async () => {
      const mockError = new Error("Failed to fetch balance");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(balanceApi.getBalance()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
