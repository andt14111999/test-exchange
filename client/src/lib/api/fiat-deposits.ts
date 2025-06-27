import { apiClient } from "./client";
import {
  FiatDeposit,
  CreateFiatDepositParams,
  OwnershipVerificationParams,
} from "@/types/fiat-deposits";
import { API_ENDPOINTS } from "./config";

// Tạo deposit cho một trade cụ thể
export const createTradeRelatedDeposit = async (
  tradeId: string,
  params: CreateFiatDepositParams,
): Promise<FiatDeposit> => {
  const response = await apiClient.post(
    API_ENDPOINTS.trades.fiatDeposit.create(tradeId),
    params,
  );
  return response.data;
};

// Lấy chi tiết một deposit
export const getFiatDeposit = async (id: string): Promise<FiatDeposit> => {
  const response = await apiClient.get(API_ENDPOINTS.fiat.deposit.get(id));
  return response.data;
};

// Lấy chi tiết deposit của một trade
export const getTradeFiatDeposit = async (
  tradeId: string,
): Promise<FiatDeposit> => {
  const response = await apiClient.get(
    API_ENDPOINTS.trades.fiatDeposit.get(tradeId),
  );
  return response.data;
};

// Cập nhật interface cho payment proof
interface PaymentProofParams {
  payment_proof_url: string;
  payment_description?: string;
  mark_as_sent?: boolean;
  additional_proof?: boolean;
}

/**
 * Mark a deposit as money sent with payment proof
 */
export const markMoneySent = async (
  id: string,
  paymentProof?: PaymentProofParams,
) => {
  const response = await apiClient.put(
    API_ENDPOINTS.fiat.deposit.markMoneySent(id),
    paymentProof,
  );
  return response.data;
};

/**
 * Mark a trade's deposit as money sent with payment proof
 */
export const markTradeDepositMoneySent = async (
  tradeId: string,
  paymentProof?: PaymentProofParams,
) => {
  const response = await apiClient.put(
    API_ENDPOINTS.trades.fiatDeposit.markMoneySent(tradeId),
    paymentProof,
  );
  return response.data;
};

// Verify quyền sở hữu
export const verifyOwnership = async (
  id: string,
  params: OwnershipVerificationParams,
): Promise<FiatDeposit> => {
  const response = await apiClient.put(
    API_ENDPOINTS.fiat.deposit.verifyOwnership(id),
    params,
  );
  return response.data;
};

/**
 * Verify ownership of a trade's deposit
 */
export const verifyTradeDepositOwnership = async (
  tradeId: string,
  params: OwnershipVerificationParams,
): Promise<FiatDeposit> => {
  const response = await apiClient.put(
    API_ENDPOINTS.trades.fiatDeposit.verifyOwnership(tradeId),
    params,
  );
  return response.data;
};

// Hủy deposit
export const cancelDeposit = async (
  id: string,
  cancelReason?: string,
): Promise<FiatDeposit> => {
  const response = await apiClient.put(
    API_ENDPOINTS.fiat.deposit.cancel(id),
    cancelReason ? { cancel_reason: cancelReason } : {},
  );
  return response.data;
};

/**
 * Cancel a trade's deposit
 */
export const cancelTradeDeposit = async (
  tradeId: string,
  cancelReason?: string,
): Promise<FiatDeposit> => {
  const response = await apiClient.put(
    API_ENDPOINTS.trades.fiatDeposit.cancel(tradeId),
    cancelReason ? { cancel_reason: cancelReason } : {},
  );
  return response.data;
};

// Get all deposits
export const getFiatDeposits = async (params?: {
  status?: string;
  currency?: string;
  page?: number;
  per_page?: number;
}): Promise<{
  data: FiatDeposit[];
  meta: { total: number; page: number; per_page: number };
}> => {
  const response = await apiClient.get(API_ENDPOINTS.fiat.deposits, { params });
  return response.data;
};

export interface FiatWithdrawal {
  id: string;
  fiat_amount: number;
  currency: string;
  status: string;
  created_at: string;
}

export const getFiatWithdrawals = async (params?: {
  status?: string;
  currency?: string;
  page?: number;
  per_page?: number;
}): Promise<{
  data: FiatWithdrawal[];
  meta: { total: number; page: number; per_page: number };
}> => {
  const response = await apiClient.get(API_ENDPOINTS.fiat.withdrawals, {
    params,
  });
  return response.data;
};
