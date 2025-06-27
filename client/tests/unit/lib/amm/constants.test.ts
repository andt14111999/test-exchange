import { BigNumber } from "bignumber.js";
import { VND, USDT, PHP, NGN, POOLS, Token } from "@/lib/amm/constants";

describe("AMM Constants", () => {
  describe("Token Constants", () => {
    it("should have correct VND token configuration", () => {
      const expectedVND: Token = {
        id: "vnd",
        symbol: "VND",
        name: "Vietnamese Dong",
        decimals: 0,
      };
      expect(VND).toEqual(expectedVND);
    });

    it("should have correct USDT token configuration", () => {
      const expectedUSDT: Token = {
        id: "usdt",
        symbol: "USDT",
        name: "USD Tether",
        decimals: 6,
      };
      expect(USDT).toEqual(expectedUSDT);
    });

    it("should have correct PHP token configuration", () => {
      const expectedPHP: Token = {
        id: "php",
        symbol: "PHP",
        name: "Philippine Peso",
        decimals: 2,
      };
      expect(PHP).toEqual(expectedPHP);
    });

    it("should have correct NGN token configuration", () => {
      const expectedNGN: Token = {
        id: "ngn",
        symbol: "NGN",
        name: "Nigerian Naira",
        decimals: 2,
      };
      expect(NGN).toEqual(expectedNGN);
    });
  });

  describe("Pool Constants", () => {
    it("should have correct number of pools", () => {
      expect(POOLS.length).toBe(3);
    });

    it("should have correct VND/USDT pool configuration", () => {
      const vndUsdtPool = POOLS.find((pool) => pool.id === "vnd-usdt");
      expect(vndUsdtPool).toBeDefined();
      expect(vndUsdtPool).toEqual({
        id: "vnd-usdt",
        name: "VND/USDT",
        token0: VND,
        token1: USDT,
        fee: 0.002,
        sqrtPriceX96: new BigNumber(25000),
        liquidity: new BigNumber(1000000),
        tickSpacing: 10,
        maxLiquidityPerTick: new BigNumber(10000000),
      });
    });

    it("should have correct PHP/USDT pool configuration", () => {
      const phpUsdtPool = POOLS.find((pool) => pool.id === "php-usdt");
      expect(phpUsdtPool).toBeDefined();
      expect(phpUsdtPool).toEqual({
        id: "php-usdt",
        name: "PHP/USDT",
        token0: PHP,
        token1: USDT,
        fee: 0.002,
        sqrtPriceX96: new BigNumber(55),
        liquidity: new BigNumber(1000000),
        tickSpacing: 10,
        maxLiquidityPerTick: new BigNumber(10000000),
      });
    });

    it("should have correct NGN/USDT pool configuration", () => {
      const ngnUsdtPool = POOLS.find((pool) => pool.id === "ngn-usdt");
      expect(ngnUsdtPool).toBeDefined();
      expect(ngnUsdtPool).toEqual({
        id: "ngn-usdt",
        name: "NGN/USDT",
        token0: NGN,
        token1: USDT,
        fee: 0.002,
        sqrtPriceX96: new BigNumber(1500),
        liquidity: new BigNumber(1000000),
        tickSpacing: 10,
        maxLiquidityPerTick: new BigNumber(10000000),
      });
    });

    it("should have all pools with valid token pairs", () => {
      POOLS.forEach((pool) => {
        expect(pool.token0).toBeDefined();
        expect(pool.token1).toBeDefined();
        expect(pool.token1).toBe(USDT); // All pairs are against USDT
        expect(pool.fee).toBe(0.002); // All pools have same fee
        expect(pool.tickSpacing).toBe(10); // All pools have same tick spacing
        expect(pool.liquidity.toString()).toBe("1000000"); // All pools have same liquidity
        expect(pool.maxLiquidityPerTick.toString()).toBe("10000000"); // All pools have same max liquidity per tick
      });
    });

    it("should have BigNumber instances for numeric values", () => {
      POOLS.forEach((pool) => {
        expect(pool.sqrtPriceX96).toBeInstanceOf(BigNumber);
        expect(pool.liquidity).toBeInstanceOf(BigNumber);
        expect(pool.maxLiquidityPerTick).toBeInstanceOf(BigNumber);
      });
    });
  });
});
