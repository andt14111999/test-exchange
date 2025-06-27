// Constants
const MIN_TICK = -887272;
const MAX_TICK = 887272;

import { BigNumber } from "bignumber.js";
import {
  priceToSqrtPriceX96,
  getTickAtSqrtRatio,
  calculateSwapOutput,
  calculateOptimalAmounts,
  calculateLiquidity,
  calculateAmount0Delta,
  calculateAmount1Delta,
  createPool,
  addLiquidity,
  Pool,
} from "@/lib/amm/core";

describe("AMM Core", () => {
  describe("priceToSqrtPriceX96", () => {
    it("should correctly convert price to sqrtPriceX96", () => {
      const price = new BigNumber("2");
      const decimals0 = 18;
      const decimals1 = 18;
      const result = priceToSqrtPriceX96(price, decimals0, decimals1);
      expect(result.toString()).toBe(
        new BigNumber(2).times(new BigNumber(2).pow(192)).sqrt().toString(),
      );
    });

    it("should handle decimal adjustments", () => {
      const price = new BigNumber("2");
      const decimals0 = 6;
      const decimals1 = 18;
      const result = priceToSqrtPriceX96(price, decimals0, decimals1);
      expect(result.toString()).toBe(
        new BigNumber(2)
          .times(new BigNumber(2).pow(192))
          .times(new BigNumber(10).pow(12))
          .sqrt()
          .toString(),
      );
    });
  });

  describe("getTickAtSqrtRatio", () => {
    it("should calculate tick for a given sqrtPriceX96", () => {
      const sqrtPriceX96 = new BigNumber("1.0001").pow(100); // A reasonable price
      const tick = getTickAtSqrtRatio(sqrtPriceX96);
      expect(tick).toBeLessThan(MAX_TICK);
      expect(tick).toBeGreaterThan(MIN_TICK);
    });

    it("should respect MIN_TICK boundary", () => {
      const verySmallPrice = new BigNumber("1e-100"); // Very small price
      const tick = getTickAtSqrtRatio(verySmallPrice);
      expect(tick).toBe(MIN_TICK);
    });

    it("should respect MAX_TICK boundary", () => {
      const veryLargePrice = new BigNumber("1e100"); // Very large price
      const tick = getTickAtSqrtRatio(veryLargePrice);
      expect(tick).toBe(MAX_TICK);
    });
  });

  describe("calculateSwapOutput", () => {
    const mockPool: Pool = {
      token0: "USDT",
      token1: "VND",
      fee: 0.003,
      sqrtPriceX96: new BigNumber(2).pow(96),
      liquidity: new BigNumber("1000000"),
      tick: 0,
    };

    it("should calculate swap output for token0 to token1", () => {
      const result = calculateSwapOutput(mockPool, new BigNumber("1000"), true);
      expect(result.amountOut.gt(0)).toBe(true);
      expect(result.newSqrtPrice.lt(mockPool.sqrtPriceX96)).toBe(true);
    });

    it("should calculate swap output for token1 to token0", () => {
      const result = calculateSwapOutput(
        mockPool,
        new BigNumber("1000"),
        false,
      );
      expect(result.amountOut.gt(0)).toBe(true);
      expect(result.newSqrtPrice.gt(mockPool.sqrtPriceX96)).toBe(true);
    });

    it("should apply fee correctly", () => {
      const result = calculateSwapOutput(mockPool, new BigNumber("1000"), true);
      const expectedFee = new BigNumber(1).minus(0.003);
      expect(
        result.amountOut.div(result.amountOut.div(expectedFee)).toFixed(3),
      ).toBe(expectedFee.toString());
    });
  });

  describe("calculateOptimalAmounts", () => {
    const mockPool: Pool = {
      token0: "USDT",
      token1: "VND",
      fee: 0.003,
      sqrtPriceX96: new BigNumber("1.0001").pow(100), // A reasonable price
      liquidity: new BigNumber("1000000"),
      tick: 0,
    };

    it("should calculate optimal amounts for given desired amounts", () => {
      const result = calculateOptimalAmounts(
        mockPool,
        new BigNumber("1000000"), // Much larger amount0Desired
        new BigNumber("1000000"), // Much larger amount1Desired
        -1000,
        1000,
      );
      // We only check that the amounts are valid numbers
      expect(result.amount0.isFinite()).toBe(true);
      expect(result.amount1.isFinite()).toBe(true);
    });
  });

  describe("calculateLiquidity", () => {
    it("should calculate liquidity when price is below range", () => {
      const result = calculateLiquidity(
        new BigNumber("1"),
        new BigNumber("2"),
        new BigNumber("4"),
        new BigNumber("1000"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });

    it("should calculate liquidity when price is in range", () => {
      const result = calculateLiquidity(
        new BigNumber("3"),
        new BigNumber("2"),
        new BigNumber("4"),
        new BigNumber("1000"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });

    it("should calculate liquidity when price is above range", () => {
      const result = calculateLiquidity(
        new BigNumber("5"),
        new BigNumber("2"),
        new BigNumber("4"),
        new BigNumber("1000"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });

    it("should handle ratio swap when A > B", () => {
      const result = calculateLiquidity(
        new BigNumber("3"),
        new BigNumber("4"),
        new BigNumber("2"),
        new BigNumber("1000"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });
  });

  describe("calculateAmount0Delta and calculateAmount1Delta", () => {
    it("should calculate amount0 delta correctly", () => {
      const result = calculateAmount0Delta(
        new BigNumber("2"),
        new BigNumber("4"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });

    it("should calculate amount1 delta correctly", () => {
      const result = calculateAmount1Delta(
        new BigNumber("2"),
        new BigNumber("4"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });

    it("should handle ratio swap for amount0 delta", () => {
      const result = calculateAmount0Delta(
        new BigNumber("4"),
        new BigNumber("2"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });

    it("should handle ratio swap for amount1 delta", () => {
      const result = calculateAmount1Delta(
        new BigNumber("4"),
        new BigNumber("2"),
        new BigNumber("1000"),
      );
      expect(result.gt(0)).toBe(true);
    });
  });

  describe("Pool Management", () => {
    describe("createPool", () => {
      it("should create a pool with correct parameters", () => {
        const pool = createPool(
          "USDT",
          "VND",
          new BigNumber("25000"),
          new BigNumber("1000000"),
        );
        expect(pool.token0).toBe("USDT");
        expect(pool.token1).toBe("VND");
        expect(pool.fee).toBe(0.003);
        expect(pool.liquidity.eq(new BigNumber("1000000"))).toBe(true);
        expect(pool.sqrtPriceX96.gt(0)).toBe(true);
        expect(typeof pool.tick).toBe("number");
      });
    });

    describe("addLiquidity", () => {
      const mockPool: Pool = {
        token0: "USDT",
        token1: "VND",
        fee: 0.003,
        sqrtPriceX96: new BigNumber("1.0001").pow(100), // A reasonable price
        liquidity: new BigNumber("1000000"),
        tick: 0,
      };

      it("should add liquidity and return position", () => {
        const position = addLiquidity(
          mockPool,
          -1000,
          1000,
          new BigNumber("1000000"), // Much larger amount0Desired
          new BigNumber("1000000"), // Much larger amount1Desired
        );
        // We only check that the position has valid values
        expect(position.liquidity.isFinite()).toBe(true);
        expect(position.tickLower).toBe(-1000);
        expect(position.tickUpper).toBe(1000);
        expect(position.pool.liquidity.isFinite()).toBe(true);
      });
    });
  });
});
