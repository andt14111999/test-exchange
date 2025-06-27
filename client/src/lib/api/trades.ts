import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";
import { AxiosError } from "axios";

export interface PaymentDetails {
  bank_id?: string;
  bank_name?: string;
  bank_account_name?: string;
  bank_account_number?: string;
  bank_branch?: string;
  [key: string]: string | undefined;
}

// Interface định nghĩa kiểu dữ liệu của trade từ API
export interface ApiTrade {
  id: number | string;
  ref: string;
  status: string;
  taker_side: string;
  coin_amount: string;
  coin_currency: string;
  fiat_amount: string;
  fiat_currency: string;
  price: string;
  created_at: string;
  updated_at: string;
  paid_at?: string;
  released_at?: string;
  cancelled_at?: string;
  disputed_at?: string;
  expired_at?: string;
  seller: {
    id: number | string;
    email: string;
    display_name: string;
  };
  buyer: {
    id: number | string;
    email: string;
    display_name: string;
  };
  payment_method: string;
  payment_details: {
    bank_name?: string;
    bank_account_name?: string;
    bank_account_number?: string;
    [key: string]: string | undefined;
  };
  payment_receipt_details?: {
    proof_url?: string;
    file_url?: string;
    description?: string;
    uploaded_at?: string;
    file?: File;
  };
  countdown_status?: string;
  countdown_seconds?: number;
  unpaid_timeout_at?: string;
  paid_timeout_at?: string;
  payment_time?: number;
  trade_memo?: string;
  dispute_reason?: string;
  dispute_resolution?: string;
  // New fee-related properties
  total_fee?: string;
  amount_after_fee?: string;
  fee_ratio?: string;
  fixed_fee?: string;
  coin_trading_fee?: string;
}

export interface TradeListParams {
  status?: string;
  role?: "buyer" | "seller";
  page?: number;
  per_page?: number;
}

// Get all trades with optional filters
export const getTrades = async (
  params?: TradeListParams,
): Promise<ApiTrade[]> => {
  const response = await apiClient.get(API_ENDPOINTS.trades.list, { params });
  return response.data;
};

// Get a single trade by ID
export const getTrade = async (id: string): Promise<ApiTrade> => {
  const response = await apiClient.get(API_ENDPOINTS.trades.get(id));
  return response.data;
};

export interface CreateTradeParams {
  offer_id: string;
  coin_amount: number;
  bank_name?: string;
  bank_account_name?: string;
  bank_account_number?: string;
  bank_branch?: string;
}

export interface DisputeTradeParams {
  dispute_reason: string;
}

export interface CancelTradeParams {
  cancel_reason: string;
}

// Create a new trade
export const createTrade = async (
  params: CreateTradeParams,
): Promise<ApiTrade> => {
  const response = await apiClient.post(API_ENDPOINTS.trades.create, params);
  return response.data;
};

// Mark a trade as paid
export const markTradePaid = async (
  id: string,
  payment_receipt_details: {
    file: File;
    description: string;
  },
): Promise<ApiTrade> => {
  try {
    // Validate file before sending
    if (!payment_receipt_details.file) {
      throw new Error("Payment proof file is required");
    }

    // Validate file size (e.g., max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (payment_receipt_details.file.size > maxSize) {
      throw new Error(
        `File size too large. Maximum allowed: ${maxSize / (1024 * 1024)}MB`,
      );
    }

    // Validate file type
    const allowedTypes = [
      "image/jpeg",
      "image/jpg",
      "image/png",
      "image/gif",
      "image/webp",
    ];
    if (!allowedTypes.includes(payment_receipt_details.file.type)) {
      throw new Error(
        `Invalid file type. Allowed types: ${allowedTypes.join(", ")}`,
      );
    }

    // Create FormData to send file
    const formData = new FormData();
    formData.append(
      "payment_receipt_details[file]",
      payment_receipt_details.file,
    );
    formData.append(
      "payment_receipt_details[description]",
      payment_receipt_details.description,
    );

    const response = await apiClient.put(
      API_ENDPOINTS.trades.markPaid(id),
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );

    return response.data;
  } catch (error) {
    // Check if it's an Axios error with response data
    if (error && typeof error === "object" && "response" in error) {
      const axiosError = error as AxiosError;

      // Extract specific error message from backend
      if (
        axiosError.response?.data &&
        typeof axiosError.response.data === "object" &&
        "error" in axiosError.response.data
      ) {
        const errorData = axiosError.response.data as { error: string };
        throw new Error(`Backend error: ${errorData.error}`);
      }
    }

    // Provide more specific error messages
    if (error instanceof Error) {
      if (error.message.includes("Failed to process payment receipt file")) {
        throw new Error(
          "File upload failed. Please check file size and format, then try again.",
        );
      }
      throw error;
    }

    throw new Error("Failed to mark trade as paid. Please try again.");
  }
};

// Release funds for a trade
export const releaseTrade = async (id: string): Promise<ApiTrade> => {
  const response = await apiClient.post(API_ENDPOINTS.trades.release(id));
  return response.data;
};

// Dispute a trade
export const disputeTrade = async (
  id: string,
  params: DisputeTradeParams,
): Promise<ApiTrade> => {
  const response = await apiClient.put(
    API_ENDPOINTS.trades.dispute(id),
    params,
  );
  return response.data;
};

// Cancel a trade
export const cancelTrade = async (
  id: string,
  params: CancelTradeParams,
): Promise<ApiTrade> => {
  const response = await apiClient.put(API_ENDPOINTS.trades.cancel(id), params);
  return response.data;
};
