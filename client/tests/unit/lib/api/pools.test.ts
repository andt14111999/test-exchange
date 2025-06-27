import { BigNumber } from "bignumber.js";
import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  fetchPools,
  fetchPoolByPair,
  fetchActivePools,
  formatApiPool,
  type ApiPool,
  type ApiPoolsResponse,
  type ActivePoolsResponse,
} from "@/lib/api/pools";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Pools API", () => {
  const mockApiPool: ApiPool = {
    id: 1,
    pair: "btc_usdt",
    token0: "btc",
    token1: "usdt",
    tick_spacing: 10,
    fee_percentage: "0.003",
    current_tick: 100,
    sqrt_price: "1000000000",
    price: "20000",
    apr: 5.5,
    tvl_in_token0: "10",
    tvl_in_token1: "200000",
    created_at: 1647734400,
    updated_at: 1647734400,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("formatApiPool", () => {
    it("should correctly format API pool data", () => {
      const formattedPool = formatApiPool(mockApiPool);

      expect(formattedPool).toEqual({
        id: 1,
        pair: "btc_usdt",
        name: "BTC/USDT",
        token0: "btc",
        token1: "usdt",
        fee: 0.003,
        tickSpacing: 10,
        currentTick: 100,
        price: new BigNumber("20000"),
        sqrtPriceX96: new BigNumber("1000000000"),
        apr: 5.5,
        liquidity: new BigNumber("10"),
      });
    });

    it("should handle missing or zero values", () => {
      const poolWithMissingValues: ApiPool = {
        ...mockApiPool,
        price: "",
        sqrt_price: "",
        tvl_in_token0: "",
      };

      const formattedPool = formatApiPool(poolWithMissingValues);

      expect(formattedPool.price).toEqual(new BigNumber("0"));
      expect(formattedPool.sqrtPriceX96).toEqual(new BigNumber("0"));
      expect(formattedPool.liquidity).toEqual(new BigNumber("0"));
    });
  });

  describe("fetchPools", () => {
    it("should fetch pools successfully", async () => {
      const mockResponse: ApiPoolsResponse = {
        amm_pools: [mockApiPool],
        meta: {
          current_page: 1,
          next_page: null,
          total_pages: 1,
          per_page: 10,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await fetchPools();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.pools);
      expect(result).toEqual([mockApiPool]);
    });

    it("should return empty array on error", async () => {
      const mockError = new Error("Failed to fetch pools");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      const result = await fetchPools();

      expect(consoleSpy).toHaveBeenCalledWith(
        "Error fetching pools:",
        mockError,
      );
      expect(result).toEqual([]);
    });
  });

  describe("fetchPoolByPair", () => {
    it("should fetch pool by pair successfully", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({ data: mockApiPool });

      const result = await fetchPoolByPair("btc_usdt");

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.amm.poolDetail("btc_usdt"),
      );
      expect(result).toEqual(formatApiPool(mockApiPool));
    });

    it("should return null on error", async () => {
      const mockError = new Error("Failed to fetch pool");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      const result = await fetchPoolByPair("btc_usdt");

      expect(consoleSpy).toHaveBeenCalledWith(
        "Error fetching pool with pair btc_usdt:",
        mockError,
      );
      expect(result).toBeNull();
    });
  });

  describe("fetchActivePools", () => {
    it("should fetch active pools successfully", async () => {
      const mockResponse: ActivePoolsResponse = {
        pools: [
          {
            pair: "btc_usdt",
            token0: "btc",
            token1: "usdt",
          },
        ],
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await fetchActivePools();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.activePools);
      expect(result).toEqual(mockResponse.pools);
    });

    it("should return empty array on error", async () => {
      const mockError = new Error("Failed to fetch active pools");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      const result = await fetchActivePools();

      expect(consoleSpy).toHaveBeenCalledWith(
        "Error fetching active pools:",
        mockError,
      );
      expect(result).toEqual([]);
    });
  });
});
