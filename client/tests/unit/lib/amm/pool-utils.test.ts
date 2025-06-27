import {
  getUniqueTokens,
  findPoolPair,
  formatPoolPrice,
} from "@/lib/amm/pool-utils";
import { ActivePool } from "@/lib/api/pools";

describe("pool-utils", () => {
  describe("getUniqueTokens", () => {
    it("should return empty lists when input is empty", () => {
      expect(getUniqueTokens([])).toEqual({
        token0List: [],
        token1List: [],
      });
    });

    it("should return empty lists when input is null or undefined", () => {
      expect(getUniqueTokens(null as unknown as ActivePool[])).toEqual({
        token0List: [],
        token1List: [],
      });
      expect(getUniqueTokens(undefined as unknown as ActivePool[])).toEqual({
        token0List: [],
        token1List: [],
      });
    });

    it("should return unique tokens from pools", () => {
      const pools: ActivePool[] = [
        { token0: "usdt", token1: "vnd" } as ActivePool,
        { token0: "usdt", token1: "btc" } as ActivePool,
        { token0: "eth", token1: "vnd" } as ActivePool,
      ];

      const result = getUniqueTokens(pools);

      expect(result.token0List).toEqual(["USDT", "ETH"]);
      expect(result.token1List).toEqual(["VND", "BTC"]);
    });

    it("should handle case-insensitive duplicates", () => {
      const pools: ActivePool[] = [
        { token0: "usdt", token1: "vnd" } as ActivePool,
        { token0: "USDT", token1: "BTC" } as ActivePool,
      ];

      const result = getUniqueTokens(pools);

      expect(result.token0List).toEqual(["USDT"]);
      expect(result.token1List).toEqual(["VND", "BTC"]);
    });
  });

  describe("findPoolPair", () => {
    const pools: ActivePool[] = [
      { token0: "usdt", token1: "vnd" } as ActivePool,
      { token0: "eth", token1: "btc" } as ActivePool,
    ];

    it("should return null when pools array is empty", () => {
      expect(findPoolPair([], "usdt", "vnd")).toBeNull();
    });

    it("should return null when pools is null or undefined", () => {
      expect(
        findPoolPair(null as unknown as ActivePool[], "usdt", "vnd"),
      ).toBeNull();
      expect(
        findPoolPair(undefined as unknown as ActivePool[], "usdt", "vnd"),
      ).toBeNull();
    });

    it("should find pool with exact token order", () => {
      const result = findPoolPair(pools, "usdt", "vnd");
      expect(result).toBe(pools[0]);
    });

    it("should find pool with reversed token order", () => {
      const result = findPoolPair(pools, "vnd", "usdt");
      expect(result).toBe(pools[0]);
    });

    it("should handle case-insensitive search", () => {
      const result = findPoolPair(pools, "USDT", "VND");
      expect(result).toBe(pools[0]);
    });

    it("should return null when pool not found", () => {
      const result = findPoolPair(pools, "usdt", "btc");
      expect(result).toBeNull();
    });
  });

  describe("formatPoolPrice", () => {
    it("should format price > 1 with 2 decimal places", () => {
      const result = formatPoolPrice("1234.5678");
      expect(result).toBe("1,234.57");
    });

    it("should format price < 1 with 6 decimal places", () => {
      const result = formatPoolPrice("0.123456789");
      expect(result).toBe("0.123457");
    });

    it("should handle string input", () => {
      const result = formatPoolPrice("123");
      expect(result).toBe("123.00");
    });

    it("should handle number input", () => {
      const result = formatPoolPrice(123);
      expect(result).toBe("123.00");
    });

    it("should return '0' for invalid number strings", () => {
      const result = formatPoolPrice("invalid");
      expect(result).toBe("0");
    });

    it("should return '0' for NaN", () => {
      const result = formatPoolPrice(NaN);
      expect(result).toBe("0");
    });

    it("should handle errors and return '0'", () => {
      // Mock toLocaleString to throw an error
      const mockNumber = {
        toLocaleString: jest.fn().mockImplementation(() => {
          throw new Error("Mock error");
        }),
      };
      const result = formatPoolPrice(mockNumber as unknown as number);
      expect(result).toBe("0");
    });
  });
});
