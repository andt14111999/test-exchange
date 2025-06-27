import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface CoinAccount {
  coin_currency: string;
  balance: number;
  frozen_balance: number;
}

export interface FiatAccount {
  currency: string;
  balance: number;
  frozen_balance: number;
}

export interface BalanceData {
  coin_accounts: CoinAccount[];
  fiat_accounts: FiatAccount[];
}

export interface BalanceResponse {
  status: string;
  data: BalanceData;
}

export const balanceApi = {
  getBalance: async () => {
    const response = await apiClient.get<BalanceResponse>(
      API_ENDPOINTS.wallet.balances,
    );
    return response.data;
  },
};
