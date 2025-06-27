import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

// Interface cho Position
export interface AmmPosition {
  id: number;
  identifier: string;
  pool_pair: string;
  tick_lower_index: number;
  tick_upper_index: number;
  status: "pending" | "open" | "closed" | "error";
  error_message: string | null;

  liquidity: string;
  amount0: string;
  amount1: string;
  amount0_initial: string;
  amount1_initial: string;
  slippage: string;

  fee_growth_inside0_last: string;
  fee_growth_inside1_last: string;
  tokens_owed0: string;
  tokens_owed1: string;
  fee_collected0: string;
  fee_collected1: string;

  // Additional fields for detail view
  amount0_withdrawal?: string;
  amount1_withdrawal?: string;
  estimate_fee_token0?: string;
  estimate_fee_token1?: string;
  apr?: string;

  // Total estimated fee converted to token0 for easier frontend display
  total_estimate_fee_in_token0?: string;

  created_at: number;
  updated_at: number;
}

export interface AmmPositionsResponse {
  amm_positions: AmmPosition[];
  meta: {
    current_page: number;
    next_page: number | null;
    total_pages: number;
    per_page: number;
  };
}

export interface AmmPositionResponse {
  amm_position: AmmPosition;
}

export interface CreatePositionParams {
  pool_pair: string;
  tick_lower_index: number;
  tick_upper_index: number;
  amount0_initial: string | number;
  amount1_initial: string | number;
  slippage: number;
}

// Lấy danh sách vị thế
export const fetchPositions = async (
  status: "pending" | "open" | "closed" | "error" | "all" = "open",
  page: number = 1,
  perPage: number = 10,
): Promise<AmmPositionsResponse> => {
  try {
    const response = await apiClient.get<AmmPositionsResponse>(
      API_ENDPOINTS.amm.positions,
      {
        params: {
          status,
          page,
          per_page: perPage,
        },
      },
    );
    return response.data;
  } catch (error) {
    console.error("Error fetching positions:", error);
    throw error;
  }
};

// Lấy chi tiết một vị thế
export const fetchPositionById = async (
  id: number,
): Promise<AmmPositionResponse> => {
  try {
    const response = await apiClient.get<AmmPositionResponse>(
      API_ENDPOINTS.amm.positionDetail(id),
    );
    return response.data;
  } catch (error) {
    console.error(`Error fetching position with id ${id}:`, error);
    throw error;
  }
};

// Tạo vị thế mới
export const createPosition = async (
  params: CreatePositionParams,
): Promise<AmmPositionResponse> => {
  try {
    const response = await apiClient.post<AmmPositionResponse>(
      API_ENDPOINTS.amm.positions,
      params,
    );
    return response.data;
  } catch (error) {
    console.error("Error creating position:", error);
    throw error;
  }
};

// Thu phí từ vị thế
export const collectFee = async (id: number): Promise<unknown> => {
  try {
    const response = await apiClient.post(API_ENDPOINTS.amm.collectFee(id));
    return response.data;
  } catch (error) {
    console.error(`Error collecting fee for position ${id}:`, error);
    throw error;
  }
};

// Đóng vị thế
export const closePosition = async (id: number): Promise<unknown> => {
  try {
    const response = await apiClient.post(API_ENDPOINTS.amm.closePosition(id));
    return response.data;
  } catch (error) {
    console.error(`Error closing position ${id}:`, error);
    throw error;
  }
};
