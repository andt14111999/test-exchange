import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface CreateWithdrawalRequest {
  coin_amount: number;
  coin_currency: string;
  coin_layer: string;
  coin_address?: string;
  receiver_username?: string;
}

export interface WithdrawalResponse {
  id: string;
  coin_amount: number;
  coin_fee: number;
  coin_currency: string;
  coin_layer: string;
  coin_address: string;
  status: "PENDING" | "COMPLETED" | "FAILED" | "CANCELLED";
  created_at: string;
  tx_hash?: string; // Blockchain transaction hash (might be null initially)
  network_name?: string; // Human-readable network name
  is_internal_transfer: boolean;
  receiver_username?: string;
}

export interface CheckReceiverResponse {
  valid: boolean;
}

export async function createWithdrawal(
  data: CreateWithdrawalRequest,
): Promise<WithdrawalResponse> {
  const response = await apiClient.post(API_ENDPOINTS.withdrawals.create, data);
  return response.data.data;
}

export async function getWithdrawalById(
  id: string,
): Promise<WithdrawalResponse> {
  const response = await apiClient.get(API_ENDPOINTS.withdrawals.get(id));
  return response.data.data;
}

export async function checkReceiver(
  receiverUsername: string,
): Promise<boolean> {
  const response = await apiClient.get("/coin_withdrawals/check_receiver", {
    params: {
      receiver_username: receiverUsername,
    },
  });
  return response.data;
}
