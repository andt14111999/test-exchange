import { apiClient } from "@/lib/api/client";
import { fetchWalletData } from "@/lib/api/coin-accounts";
import { API_ENDPOINTS } from "@/lib/api/config";
import type { WalletData } from "@/types";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Coin Accounts API", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchWalletData", () => {
    it("should fetch wallet data successfully", async () => {
      const mockWalletData: WalletData = {
        coin_accounts: [
          {
            id: 1,
            user_id: 1,
            coin_currency: "BTC",
            balance: 1.5,
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
            balance: 1000,
            frozen_balance: 100,
            created_at: "2024-03-20T00:00:00Z",
            updated_at: "2024-03-20T00:00:00Z",
          },
        ],
      };

      const mockResponse = {
        data: {
          status: "success",
          data: mockWalletData,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await fetchWalletData();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.wallet.balances);
      expect(result).toEqual(mockWalletData);
    });

    it("should handle error when fetching wallet data fails", async () => {
      const mockError = new Error("Failed to fetch wallet data");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(fetchWalletData()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
