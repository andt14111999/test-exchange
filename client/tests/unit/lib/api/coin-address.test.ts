import { apiClient } from "@/lib/api/client";
import { fetchCoinAddress, generateCoinAddress } from "@/lib/api/coin-address";
import { API_ENDPOINTS } from "@/lib/api/config";
import type { CoinAddressResponse } from "@/types/coin-address";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

describe("Coin Address API", () => {
  const mockCoinCurrency = "BTC";
  const mockLayer = "BTC";
  const mockAddressResponse: CoinAddressResponse = {
    status: "success",
    data: {
      address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
      network: "Bitcoin",
      coin_currency: "BTC",
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchCoinAddress", () => {
    it("should fetch coin address successfully", async () => {
      const mockResponse = {
        data: mockAddressResponse,
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await fetchCoinAddress(mockCoinCurrency, mockLayer);

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.coinAccounts.address(mockCoinCurrency, mockLayer),
      );
      expect(result).toEqual(mockAddressResponse);
    });

    it("should handle error when fetching coin address fails", async () => {
      const mockError = new Error("Failed to fetch coin address");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        fetchCoinAddress(mockCoinCurrency, mockLayer),
      ).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("generateCoinAddress", () => {
    it("should generate coin address successfully", async () => {
      const mockResponse = {
        data: mockAddressResponse,
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await generateCoinAddress(mockCoinCurrency, mockLayer);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.coinAccounts.generateAddress(mockCoinCurrency, mockLayer),
      );
      expect(result).toEqual(mockAddressResponse);
    });

    it("should handle error when generating coin address fails", async () => {
      const mockError = new Error("Failed to generate coin address");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        generateCoinAddress(mockCoinCurrency, mockLayer),
      ).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });
});
