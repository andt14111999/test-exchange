import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface SwapParams {
  poolPair: string;
  zeroForOne: boolean;
  amountSpecified: string;
  skipFetchPool?: boolean;
  amountEstimated: string;
  slippage?: number;
}

export interface SwapResponse {
  success: boolean;
  transaction_id?: string;
  error?: string;
}

export interface SwapOrder {
  id: number;
  identifier: string;
  zero_for_one: boolean;
  status: string;
  error_message: string | null;
  before_tick_index: number;
  after_tick_index: number;
  amount_specified: string;
  amount_estimated: string;
  amount_actual: string;
  amount_received: string;
  slippage: string;
  fees: Record<string, number>;
  created_at: number;
  updated_at: number;
}

export interface SwapOrdersResponse {
  amm_orders: SwapOrder[];
  meta: {
    current_page: number;
    next_page: number | null;
    total_pages: number;
    per_page: number;
  };
}

export interface SwapOrderParams {
  poolPair: string;
  zeroForOne: boolean;
  amountSpecified: string;
  amountEstimated: string;
  slippage: number;
}

/**
 * Thực hiện giao dịch swap
 */
export const executeSwap = async (
  params: SwapOrderParams,
): Promise<SwapOrder> => {
  try {
    const payload = {
      pool_pair: params.poolPair,
      zero_for_one: params.zeroForOne,
      amount_specified: params.amountSpecified,
      amount_estimated: params.amountEstimated,
      slippage: params.slippage,
    };

    const response = await apiClient.post<SwapOrder>(
      API_ENDPOINTS.amm.orders,
      payload,
    );

    return response.data;
  } catch (error) {
    console.error("Error executing swap:", error);
    throw error;
  }
};

/**
 * Lấy danh sách lịch sử giao dịch swap của người dùng
 */
export const fetchSwapOrders = async (
  page = 1,
  perPage = 10,
  status?: string,
): Promise<SwapOrdersResponse> => {
  try {
    const params: {
      page: number;
      per_page: number;
      status?: string;
    } = {
      page,
      per_page: perPage,
    };

    // Thêm status vào params nếu có
    if (status && status !== "all") {
      params.status = status;
    }

    const response = await apiClient.get<SwapOrdersResponse>(
      API_ENDPOINTS.amm.orders,
      {
        params,
      },
    );

    // Đảm bảo response có cấu trúc đúng
    if (!response.data || !response.data.amm_orders) {
      return {
        amm_orders: [],
        meta: {
          current_page: page,
          next_page: null,
          total_pages: 1,
          per_page: perPage,
        },
      };
    }

    return response.data;
  } catch (error) {
    console.error("Error fetching swap orders:", error);
    // Trả về dữ liệu rỗng nếu có lỗi
    return {
      amm_orders: [],
      meta: {
        current_page: page,
        next_page: null,
        total_pages: 1,
        per_page: perPage,
      },
    };
  }
};

/**
 * Lấy chi tiết giao dịch swap theo ID
 */
export const fetchSwapOrderDetail = async (id: number): Promise<SwapOrder> => {
  try {
    const response = await apiClient.get<SwapOrder>(
      `${API_ENDPOINTS.amm.orders}/${id}`,
    );

    return response.data;
  } catch (error) {
    console.error("Error fetching swap order detail:", error);
    throw error;
  }
};
