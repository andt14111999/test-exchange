import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface Tick {
  tick_index: number;
  liquidity_gross: string;
  liquidity_net: string;
  fee_growth_outside0: string;
  fee_growth_outside1: string;
  initialized: boolean;
}

export interface TicksResponse {
  [poolPair: string]: {
    ticks: Tick[];
  };
}

/**
 * Lấy dữ liệu ticks cho một cặp pool
 */
export const fetchTicks = async (poolPair: string): Promise<Tick[]> => {
  try {
    const response = await apiClient.get<TicksResponse>(
      API_ENDPOINTS.amm.ticks,
      {
        params: { pool_pair: poolPair },
      },
    );

    // Kiểm tra và trả về dữ liệu ticks
    if (
      response.data &&
      response.data[poolPair] &&
      response.data[poolPair].ticks
    ) {
      return response.data[poolPair].ticks;
    }

    // Trả về mảng rỗng nếu không có dữ liệu
    return [];
  } catch (error) {
    console.error(`Error fetching ticks for pool ${poolPair}:`, error);
    return [];
  }
};
