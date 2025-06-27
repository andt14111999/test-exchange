import { WalletData } from "@/types";
import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

interface WalletResponse {
  status: string;
  data: WalletData;
}

export async function fetchWalletData(): Promise<WalletData> {
  const response = await apiClient.get<WalletResponse>(
    API_ENDPOINTS.wallet.balances,
  );
  return response.data.data;
}
