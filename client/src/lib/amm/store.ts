import { create } from "zustand";
import type { StateCreator } from "zustand";
import { BigNumber } from "bignumber.js";
import {
  Pool,
  Position,
  createPool,
  addLiquidity,
  calculateSwapOutput,
} from "./core";
import { AMMPosition } from "./types";

interface AMMState {
  pools: Record<string, Pool>;
  positions: Position[];
  // Actions
  initializePool: (
    poolId: string,
    token0: string,
    token1: string,
    initialPrice: string,
    initialLiquidity: string,
  ) => void;
  addLiquidityPosition: (position: AMMPosition) => void;
  swap: (
    poolId: string,
    amountIn: string,
    zeroForOne: boolean,
  ) => { amountOut: string; newPrice: string };
}

const initialState = {
  positions: [
    {
      pool: {
        token0: "USDT",
        token1: "VND",
        sqrtPriceX96: new BigNumber("23000"),
        liquidity: new BigNumber("1000"),
        tick: 73500,
        fee: 0.003,
      },
      tickLower: 73000,
      tickUpper: 75000,
      liquidity: new BigNumber("1000"),
    },
    {
      pool: {
        token0: "USDT",
        token1: "PHP",
        sqrtPriceX96: new BigNumber("55"),
        liquidity: new BigNumber("2000"),
        tick: 70500,
        fee: 0.003,
      },
      tickLower: 70000,
      tickUpper: 71000,
      liquidity: new BigNumber("2000"),
    },
  ],
  // ... rest of your initial state
};

export const createAMMStore: StateCreator<AMMState> = (set, get) => ({
  pools: {},
  positions: initialState.positions,

  initializePool: (
    poolId: string,
    token0: string,
    token1: string,
    initialPrice: string,
    initialLiquidity: string,
  ) => {
    const pool = createPool(
      token0,
      token1,
      new BigNumber(initialPrice),
      new BigNumber(initialLiquidity),
    );

    set((state) => ({
      pools: {
        ...state.pools,
        [poolId]: pool,
      },
    }));
  },

  addLiquidityPosition: (position: AMMPosition) => {
    // Tách token từ pool_pair (ví dụ: "usdt_vnd" => ["usdt", "vnd"])
    const [token0, token1] = position.pool_pair.toLowerCase().split("_");

    // Tìm hoặc tạo pool mới nếu không tồn tại
    let pool = get().pools[position.pool_pair];

    if (!pool) {
      // Khởi tạo pool mới với giá ước tính từ tick
      const initialPrice = Math.pow(
        1.0001,
        (position.tick_lower_index + position.tick_upper_index) / 2,
      );
      pool = createPool(
        token0,
        token1,
        new BigNumber(initialPrice.toString()),
        new BigNumber("0"), // Bắt đầu với liquidity = 0
      );
    }

    // Tạo position sử dụng hàm addLiquidity từ core
    const newPosition = addLiquidity(
      pool,
      position.tick_lower_index,
      position.tick_upper_index,
      new BigNumber(position.amount0_initial),
      new BigNumber(position.amount1_initial),
    );

    // Cập nhật state
    set((state) => ({
      pools: {
        ...state.pools,
        [position.pool_pair]: newPosition.pool,
      },
      positions: [...state.positions, newPosition],
    }));
  },

  swap: (poolId: string, amountIn: string, zeroForOne: boolean) => {
    const pool = get().pools[poolId];
    if (!pool) {
      return {
        amountOut: "0",
        newPrice: "0",
      };
    }

    const { amountOut, newSqrtPrice } = calculateSwapOutput(
      pool,
      new BigNumber(amountIn),
      zeroForOne,
    );

    // Update pool state with new price and potentially new liquidity
    set((state) => ({
      pools: {
        ...state.pools,
        [poolId]: {
          ...pool,
          sqrtPriceX96: newSqrtPrice,
        },
      },
    }));

    return {
      amountOut: amountOut.toString(),
      newPrice: newSqrtPrice.toString(),
    };
  },
});

export const useAMMStore = create<AMMState>(createAMMStore);
