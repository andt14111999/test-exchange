import { LiquidityCalculator } from "@/lib/amm/liquidity_calculator";
import Decimal from "decimal.js";

describe("LiquidityCalculator", () => {
  describe("tickToSqrtPrice", () => {
    it("should correctly calculate sqrt price for positive tick", () => {
      const result = LiquidityCalculator.tickToSqrtPrice(100);
      expect(result instanceof Decimal).toBe(true);
      expect(result.toNumber()).toBeGreaterThan(1);
    });

    it("should correctly calculate sqrt price for negative tick", () => {
      const result = LiquidityCalculator.tickToSqrtPrice(-100);
      expect(result instanceof Decimal).toBe(true);
      expect(result.toNumber()).toBeLessThan(1);
    });

    it("should return 1 for tick 0", () => {
      const result = LiquidityCalculator.tickToSqrtPrice(0);
      expect(result.toNumber()).toBeCloseTo(1, 10);
    });
  });

  describe("getDecimalScale", () => {
    it("should return correct scale for supported coins", () => {
      expect(LiquidityCalculator.getDecimalScale("USDT")).toBe(6);
      expect(LiquidityCalculator.getDecimalScale("VND")).toBe(2);
      expect(LiquidityCalculator.getDecimalScale("PHP")).toBe(2);
      expect(LiquidityCalculator.getDecimalScale("NGN")).toBe(2);
    });

    it("should handle case-insensitive symbols", () => {
      expect(LiquidityCalculator.getDecimalScale("usdt")).toBe(6);
      expect(LiquidityCalculator.getDecimalScale("vnd")).toBe(2);
    });

    it("should return default scale for unsupported coins", () => {
      expect(LiquidityCalculator.getDecimalScale("BTC")).toBe(6);
      expect(LiquidityCalculator.getDecimalScale("ETH")).toBe(6);
    });
  });

  describe("calculateAmounts", () => {
    const baseParams = {
      tickLower: 100,
      tickUpper: 200,
      currentTick: 150,
      amount0: null,
      amount1: null,
      token0Symbol: "USDT",
      token1Symbol: "VND",
    };

    describe("input validation", () => {
      it("should handle invalid tick range", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...baseParams,
          tickLower: 200,
          tickUpper: 100,
          amount0: 100,
        });
        expect(result).toEqual({
          amount0: "0",
          amount1: "0",
          liquidity: "0",
        });
      });

      it("should handle null or negative amounts", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...baseParams,
          amount0: -100,
          amount1: null,
        });
        expect(result).toEqual({
          amount0: "0",
          amount1: "0",
          liquidity: "0",
        });
      });
    });

    describe("currentTick < tickLower", () => {
      const lowerParams = {
        ...baseParams,
        tickLower: 150,
        tickUpper: 200,
        currentTick: 100,
      };

      it("should calculate correctly with amount0", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...lowerParams,
          amount0: 100,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount0).toBe("100");
      });

      it("should return zero when only amount1 provided", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...lowerParams,
          amount1: 100,
        });
        expect(result).toEqual({
          amount0: "0",
          amount1: "0",
          liquidity: "0",
        });
      });
    });

    describe("currentTick > tickUpper", () => {
      const upperParams = {
        ...baseParams,
        tickLower: 100,
        tickUpper: 150,
        currentTick: 200,
      };

      it("should calculate correctly with amount1", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...upperParams,
          amount1: 100,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount1).toBe("100");
      });

      it("should return zero when only amount0 provided", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...upperParams,
          amount0: 100,
        });
        expect(result).toEqual({
          amount0: "0",
          amount1: "0",
          liquidity: "0",
        });
      });
    });

    describe("currentTick within range", () => {
      const inRangeParams = {
        ...baseParams,
        tickLower: 100,
        tickUpper: 200,
        currentTick: 150,
      };

      it("should calculate correctly with amount0", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...inRangeParams,
          amount0: 100,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount0).toBe("100");
        expect(Number(result.amount1)).toBeGreaterThan(0);
      });

      it("should calculate correctly with amount1", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...inRangeParams,
          amount1: 100,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(Number(result.amount0)).toBeGreaterThan(0);
        expect(result.amount1).toBe("100");
      });
    });
  });

  describe("calculateAmountsTest", () => {
    it("should run test function without errors", () => {
      const consoleSpy = jest.spyOn(console, "log");
      LiquidityCalculator.calculateAmountsTest();
      expect(consoleSpy).toHaveBeenCalledTimes(2);
      consoleSpy.mockRestore();
    });
  });
});
