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
      expect(LiquidityCalculator.getDecimalScale("VND")).toBe(0);
      expect(LiquidityCalculator.getDecimalScale("PHP")).toBe(2);
      expect(LiquidityCalculator.getDecimalScale("NGN")).toBe(2);
    });

    it("should handle case-insensitive symbols", () => {
      expect(LiquidityCalculator.getDecimalScale("usdt")).toBe(6);
      expect(LiquidityCalculator.getDecimalScale("vnd")).toBe(0);
    });

    it("should return default scale for unsupported coins", () => {
      expect(LiquidityCalculator.getDecimalScale("BTC")).toBe(6);
      expect(LiquidityCalculator.getDecimalScale("ETH")).toBe(6);
    });
  });

  describe("calculateAmounts", () => {
    const baseParams = {
      token0Symbol: "USDT",
      token1Symbol: "VND",
    };

    describe("validation", () => {
      it("should return zero for invalid tick range", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...baseParams,
          tickLower: 200,
          tickUpper: 100,
          currentTick: 150,
          amount0: 100,
          amount1: null,
        });
        expect(result).toEqual({
          amount0: "0",
          amount1: "0",
          liquidity: "0",
        });
      });

      it("should return zero when no amounts provided", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...baseParams,
          tickLower: 100,
          tickUpper: 200,
          currentTick: 150,
          amount0: null,
          amount1: null,
        });
        expect(result).toEqual({
          amount0: "0",
          amount1: "0",
          liquidity: "0",
        });
      });

      it("should return zero when amounts are zero", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...baseParams,
          tickLower: 100,
          tickUpper: 200,
          currentTick: 150,
          amount0: 0,
          amount1: 0,
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
        tickLower: 200,
        tickUpper: 300,
        currentTick: 100,
      };

      it("should calculate correctly with amount0", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...lowerParams,
          amount0: 100,
          amount1: null,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount0).toBe("100");
        expect(Number(result.amount1)).toBeGreaterThanOrEqual(0);
      });

      it("should return zero when only amount1 provided", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...lowerParams,
          amount0: null,
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
          amount0: null,
          amount1: 100,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount1).toBe("100");
        expect(Number(result.amount0)).toBeGreaterThanOrEqual(0);
      });

      it("should return zero when only amount0 provided", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...upperParams,
          amount0: 100,
          amount1: null,
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
          amount1: null,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount0).toBe("100");
        expect(Number(result.amount1)).toBeGreaterThan(0);
      });

      it("should calculate correctly with amount1", () => {
        const result = LiquidityCalculator.calculateAmounts({
          ...inRangeParams,
          amount0: null,
          amount1: 100,
        });
        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(Number(result.amount0)).toBeGreaterThan(0);
        expect(result.amount1).toBe("100");
      });
    });

    describe("real-world scenarios", () => {
      it("should handle USDT-VND pair with realistic values", () => {
        const result = LiquidityCalculator.calculateAmounts({
          tickLower: 101650,
          tickUpper: 101750,
          currentTick: 101700,
          amount0: 50,
          amount1: null,
          token0Symbol: "USDT",
          token1Symbol: "VND",
        });

        expect(Number(result.liquidity)).toBeGreaterThan(0);
        expect(result.amount0).toBe("50");
        expect(Number(result.amount1)).toBeGreaterThan(0);
        expect(Number.isInteger(Number(result.amount1))).toBe(true);
      });

      it("should handle decimal formatting correctly", () => {
        const result = LiquidityCalculator.calculateAmounts({
          tickLower: 100,
          tickUpper: 200,
          currentTick: 150,
          amount0: 123.456789,
          amount1: null,
          token0Symbol: "USDT",
          token1Symbol: "VND",
        });

        expect(result.amount0).toBe("123.456789");
        const vndAmount = Number(result.amount1);
        expect(Number.isInteger(vndAmount)).toBe(true);
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
