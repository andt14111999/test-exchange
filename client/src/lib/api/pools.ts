import { BigNumber } from "bignumber.js";
import { API_ENDPOINTS } from "./config";
import { apiClient } from "./client";

export interface ApiPool {
  id: number;
  pair: string;
  token0: string;
  token1: string;
  tick_spacing: number;
  fee_percentage: string;
  current_tick: number;
  sqrt_price: string;
  price: string;
  apr: number;
  tvl_in_token0: string;
  tvl_in_token1: string;
  created_at: number;
  updated_at: number;
}

export interface ApiPoolsResponse {
  amm_pools: ApiPool[];
  meta: {
    current_page: number;
    next_page: number | null;
    total_pages: number;
    per_page: number;
  };
}

export interface ApiPoolDetailResponse {
  amm_pool: ApiPool;
}

export interface FormattedPool {
  id: number;
  pair: string;
  name: string;
  token0: string;
  token1: string;
  fee: number;
  tickSpacing: number;
  currentTick: number;
  price: BigNumber;
  sqrtPriceX96: BigNumber;
  apr: number;
  liquidity: BigNumber;
}

// Cấu trúc dữ liệu từ API /api/v1/amm_pools/active
export interface ActivePool {
  pair: string;
  token0: string;
  token1: string;
}

export interface ActivePoolsResponse {
  pools: ActivePool[];
}

/**
 * Format pool data từ API response
 */
export function formatApiPool(pool: ApiPool): FormattedPool {
  return {
    id: pool.id,
    pair: pool.pair,
    name: `${pool.token0.toUpperCase()}/${pool.token1.toUpperCase()}`,
    token0: pool.token0,
    token1: pool.token1,
    fee: Number(pool.fee_percentage),
    tickSpacing: pool.tick_spacing,
    currentTick: pool.current_tick,
    price: new BigNumber(pool.price || "0"),
    sqrtPriceX96: new BigNumber(pool.sqrt_price || "0"),
    apr: pool.apr,
    liquidity: new BigNumber(pool.tvl_in_token0 || "0"),
  };
}

/**
 * Fetch tất cả pools từ API
 */
export const fetchPools = async (): Promise<ApiPool[]> => {
  try {
    const response = await apiClient.get<ApiPoolsResponse>(
      API_ENDPOINTS.amm.pools,
    );
    return response.data.amm_pools;
  } catch (error) {
    console.error("Error fetching pools:", error);
    return [];
  }
};

/**
 * Fetch chi tiết pool theo cặp token
 */
export const fetchPoolByPair = async (
  pair: string,
): Promise<FormattedPool | null> => {
  try {
    const response = await apiClient.get<ApiPool>(
      API_ENDPOINTS.amm.poolDetail(pair),
    );
    return formatApiPool(response.data);
  } catch (error) {
    console.error(`Error fetching pool with pair ${pair}:`, error);
    return null;
  }
};

/**
 * Fetch danh sách active pools
 */
export const fetchActivePools = async (): Promise<ActivePool[]> => {
  try {
    const response = await apiClient.get<ActivePoolsResponse>(
      API_ENDPOINTS.amm.activePools,
    );
    return response.data.pools;
  } catch (error) {
    console.error("Error fetching active pools:", error);
    return [];
  }
};
