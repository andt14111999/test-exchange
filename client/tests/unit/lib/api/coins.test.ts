import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  fetchCoins,
  isCoin,
  getTokenBalance,
  fetchCoinSettings,
  type WalletData,
  type CoinsResponse,
  type CoinSetting,
} from "@/lib/api/coins";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Coins API", () => {
  const mockCoinsResponse: CoinsResponse = {
    coins: ["usdt", "btc", "eth"],
    fiats: ["vnd", "php", "ngn"],
  };

  const mockWalletData: WalletData = {
    coin_accounts: [
      {
        id: 1,
        user_id: 1,
        coin_currency: "btc",
        balance: 1.5,
        frozen_balance: 0,
        created_at: "2024-01-01",
        updated_at: "2024-01-01",
      },
      {
        id: 2,
        user_id: 1,
        coin_currency: "eth",
        balance: 2.5,
        frozen_balance: 0,
        created_at: "2024-01-01",
        updated_at: "2024-01-01",
      },
    ],
    fiat_accounts: [
      {
        id: 3,
        user_id: 1,
        currency: "vnd",
        balance: 1000000,
        frozen_balance: 0,
        created_at: "2024-01-01",
        updated_at: "2024-01-01",
      },
      {
        id: 4,
        user_id: 1,
        currency: "php",
        balance: 5000,
        frozen_balance: 0,
        created_at: "2024-01-01",
        updated_at: "2024-01-01",
      },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchCoins", () => {
    it("should fetch coins data successfully", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockCoinsResponse,
      });

      const result = await fetchCoins();
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.coins);
      expect(result).toEqual(mockCoinsResponse);
    });

    it("should return default data when API call fails", async () => {
      (apiClient.get as jest.Mock).mockRejectedValueOnce(
        new Error("API Error"),
      );

      const result = await fetchCoins();
      expect(result).toEqual({
        coins: ["usdt", "btc", "eth"],
        fiats: ["vnd", "php", "ngn"],
      });
    });
  });

  describe("isCoin", () => {
    it("should identify coins correctly", () => {
      expect(isCoin("btc", mockCoinsResponse)).toBe(true);
      expect(isCoin("BTC", mockCoinsResponse)).toBe(true);
      expect(isCoin("vnd", mockCoinsResponse)).toBe(false);
    });

    it("should handle case-insensitive comparison", () => {
      expect(isCoin("ETH", mockCoinsResponse)).toBe(true);
      expect(isCoin("eth", mockCoinsResponse)).toBe(true);
    });
  });

  describe("getTokenBalance", () => {
    it("should return correct coin balance", () => {
      expect(getTokenBalance("btc", mockWalletData, mockCoinsResponse)).toBe(
        "1.5",
      );
      expect(getTokenBalance("eth", mockWalletData, mockCoinsResponse)).toBe(
        "2.5",
      );
    });

    it("should return correct fiat balance", () => {
      expect(getTokenBalance("vnd", mockWalletData, mockCoinsResponse)).toBe(
        "1000000",
      );
      expect(getTokenBalance("php", mockWalletData, mockCoinsResponse)).toBe(
        "5000",
      );
    });

    it("should return '0' for non-existent token", () => {
      expect(getTokenBalance("xyz", mockWalletData, mockCoinsResponse)).toBe(
        "0",
      );
    });

    it("should return '0' when wallet data is undefined", () => {
      expect(getTokenBalance("btc", undefined, mockCoinsResponse)).toBe("0");
    });

    it("should handle case-insensitive token lookup", () => {
      expect(getTokenBalance("BTC", mockWalletData, mockCoinsResponse)).toBe(
        "1.5",
      );
      expect(getTokenBalance("VND", mockWalletData, mockCoinsResponse)).toBe(
        "1000000",
      );
    });
  });

  describe("fetchCoinSettings", () => {
    const mockCoinSettings: CoinSetting[] = [
      {
        id: 1,
        currency: "BTC",
        deposit_enabled: true,
        withdraw_enabled: true,
        swap_enabled: true,
        layers: [
          {
            layer: "BTC",
            deposit_enabled: true,
            withdraw_enabled: true,
            swap_enabled: true,
            maintenance: false,
          },
        ],
        created_at: "2024-01-01",
        updated_at: "2024-01-01",
      },
    ];

    it("should fetch coin settings successfully", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockCoinSettings,
      });

      const result = await fetchCoinSettings();
      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.settings.coinSettings,
      );
      expect(result).toEqual(mockCoinSettings);
    });

    it("should return default settings when API returns empty array", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({ data: [] });

      const result = await fetchCoinSettings();
      expect(result).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            currency: "NGN",
            deposit_enabled: true,
            withdraw_enabled: true,
          }),
        ]),
      );
    });

    it("should return default settings when API fails", async () => {
      (apiClient.get as jest.Mock).mockRejectedValueOnce(
        new Error("API Error"),
      );

      const result = await fetchCoinSettings();
      expect(result).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            currency: "NGN",
            deposit_enabled: true,
            withdraw_enabled: true,
          }),
        ]),
      );
    });
  });
});
