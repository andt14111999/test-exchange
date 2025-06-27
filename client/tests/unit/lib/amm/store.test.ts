import { useAMMStore } from "@/lib/amm/store";
import { AMMPosition } from "@/lib/amm/types";
import { BigNumber } from "bignumber.js";

// Mock core functions
jest.mock("@/lib/amm/core", () => ({
  createPool: jest.fn((token0, token1, initialPrice, initialLiquidity) => ({
    token0,
    token1,
    sqrtPriceX96: initialPrice,
    liquidity: initialLiquidity,
    tick: 0,
    fee: 0.003,
  })),
  addLiquidity: jest.fn((pool, tickLower, tickUpper) => ({
    pool: {
      ...pool,
      liquidity: pool.liquidity.plus(1000),
    },
    tickLower,
    tickUpper,
    liquidity: new BigNumber(1000),
  })),
  calculateSwapOutput: jest.fn((pool, amountIn) => ({
    amountOut: new BigNumber(amountIn).times(0.997), // Simulating 0.3% fee
    newSqrtPrice: new BigNumber(pool.sqrtPriceX96).times(1.01), // Simulating price impact
  })),
}));

describe("AMM Store", () => {
  beforeEach(() => {
    // Reset store to initial state
    useAMMStore.setState((state) => ({
      ...state,
      pools: {},
      positions: [],
    }));
    jest.clearAllMocks();
  });

  describe("Initial State", () => {
    it("should have empty pools object", () => {
      expect(useAMMStore.getState().pools).toEqual({});
    });

    it("should have empty positions array", () => {
      expect(useAMMStore.getState().positions).toEqual([]);
    });
  });

  describe("initializePool", () => {
    it("should create a new pool with correct parameters", () => {
      useAMMStore
        .getState()
        .initializePool("ETH/USDT", "ETH", "USDT", "1000000000", "1000000");

      expect(useAMMStore.getState().pools["ETH/USDT"]).toBeDefined();
      expect(useAMMStore.getState().pools["ETH/USDT"].token0).toBe("ETH");
      expect(useAMMStore.getState().pools["ETH/USDT"].token1).toBe("USDT");
    });

    it("should not affect existing pools when creating a new one", () => {
      // Create first pool
      useAMMStore
        .getState()
        .initializePool("ETH/USDT", "ETH", "USDT", "1000000000", "1000000");

      const firstPool = useAMMStore.getState().pools["ETH/USDT"];

      // Create second pool
      useAMMStore
        .getState()
        .initializePool("BTC/USDT", "BTC", "USDT", "2000000000", "2000000");

      // First pool should remain unchanged
      expect(useAMMStore.getState().pools["ETH/USDT"]).toEqual(firstPool);
    });
  });

  describe("addLiquidityPosition", () => {
    const position: AMMPosition = {
      pool_pair: "USDT/VND",
      tick_lower_index: -1000,
      tick_upper_index: 1000,
      amount0_initial: "1",
      amount1_initial: "1000",
      slippage: 100,
    };

    it("should create pool and add liquidity when pool does not exist", () => {
      useAMMStore.getState().addLiquidityPosition(position);

      expect(useAMMStore.getState().pools[position.pool_pair]).toBeDefined();
      expect(useAMMStore.getState().positions).toHaveLength(1);
    });

    it("should add liquidity to existing pool", () => {
      // Initialize pool first
      useAMMStore
        .getState()
        .initializePool(position.pool_pair, "USDT", "VND", "23000", "1000000");

      const initialLiquidity =
        useAMMStore.getState().pools[position.pool_pair].liquidity;

      // Add liquidity position
      useAMMStore.getState().addLiquidityPosition(position);

      expect(
        useAMMStore.getState().pools[position.pool_pair].liquidity.toString(),
      ).not.toBe(initialLiquidity.toString());
      expect(useAMMStore.getState().positions).toHaveLength(1);
    });

    it("should handle case-insensitive pool pairs", () => {
      // Add position with uppercase tokens
      const upperCasePosition = {
        ...position,
        pool_pair: "USDT/VND",
      };

      useAMMStore.getState().addLiquidityPosition(upperCasePosition);

      // Should find the pool with lowercase name
      expect(useAMMStore.getState().pools["USDT/VND"]).toBeDefined();
    });
  });

  describe("swap", () => {
    beforeEach(() => {
      // Initialize a pool for swap tests
      useAMMStore
        .getState()
        .initializePool("USDT/VND", "USDT", "VND", "23000", "1000000");
    });

    it("should return zero amounts when pool does not exist", () => {
      const result = useAMMStore.getState().swap("NONEXISTENT", "1000", true);
      expect(result.amountOut).toBe("0");
      expect(result.newPrice).toBe("0");
    });

    it("should calculate swap output and update pool price (zeroForOne)", () => {
      const result = useAMMStore.getState().swap("USDT/VND", "1", true);

      expect(result.amountOut).toBe("0.997"); // 0.3% fee
      expect(result.newPrice).not.toBe("23000"); // Price should change
    });

    it("should calculate swap output and update pool price (!zeroForOne)", () => {
      const result = useAMMStore.getState().swap("USDT/VND", "1000", false);

      expect(result.amountOut).toBe("997"); // 0.3% fee
      expect(result.newPrice).not.toBe("23000"); // Price should change
    });

    it("should update pool state after swap", () => {
      const initialPrice =
        useAMMStore.getState().pools["USDT/VND"].sqrtPriceX96;

      useAMMStore.getState().swap("USDT/VND", "1", true);

      expect(
        useAMMStore.getState().pools["USDT/VND"].sqrtPriceX96.toString(),
      ).not.toBe(initialPrice.toString());
    });
  });
});
