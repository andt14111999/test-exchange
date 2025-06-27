import { useQuery } from "@tanstack/react-query";
import { API_ENDPOINTS } from "@/lib/api/config";
import { useUserStore } from "@/lib/store/user-store";
import { apiClient } from "@/lib/api/client";
import { useBalanceStore } from "@/lib/store/balance-store";
import { BalanceData } from "@/lib/api/balance";
import { useEffect, useState } from "react";

export interface CoinAccount {
  id: number;
  user_id: number;
  coin_currency: string;
  balance: number;
  frozen_balance: number;
  created_at: string;
  updated_at: string;
}

export interface FiatAccount {
  id: number;
  user_id: number;
  currency: string;
  balance: number;
  frozen_balance: number;
  created_at: string;
  updated_at: string;
}

export interface WalletData {
  coin_accounts: CoinAccount[];
  fiat_accounts: FiatAccount[];
}

const convertBalanceToWallet = (
  balanceData: BalanceData,
  userId?: string,
): WalletData => {
  return {
    coin_accounts: balanceData.coin_accounts.map((account) => ({
      id: 0, // These fields are not available in balance data
      user_id: Number(userId || 0),
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      ...account,
    })),
    fiat_accounts: balanceData.fiat_accounts.map((account) => ({
      id: 0, // These fields are not available in balance data
      user_id: Number(userId || 0),
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      ...account,
    })),
  };
};

const fetchWalletData = async (): Promise<WalletData> => {
  const response = await apiClient.get(API_ENDPOINTS.wallet.balances);
  return response.data.data;
};

export function useWallet() {
  const { user } = useUserStore();
  const balanceData = useBalanceStore((state) => state.balanceData);
  const balanceUpdated = useBalanceStore((state) => state.balanceUpdated);

  const [localUpdated, setLocalUpdated] = useState(0);

  useEffect(() => {
    if (balanceUpdated) {
      console.log("💰 Balance updated in wallet hook, triggering rerender");
      setLocalUpdated((prev) => prev + 1);
    }
  }, [balanceUpdated]);

  const query = useQuery({
    queryKey: ["wallet", user?.id, localUpdated],
    queryFn: fetchWalletData,
    enabled: !!user,
    staleTime: 0, // Always consider the data stale to ensure fresh data
    gcTime: 0, // Don't cache the data to ensure fresh data on page navigation
  });

  // Always use the latest balance data from the store if available
  const data = balanceData
    ? convertBalanceToWallet(balanceData, user?.id)
    : query.data;

  return {
    ...query,
    data,
    isLoading: query.isLoading && !balanceData,
  };
}
