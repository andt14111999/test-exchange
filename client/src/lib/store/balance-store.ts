import { create } from "zustand";
import type { BalanceData, CoinAccount, FiatAccount } from "@/lib/api/balance";

interface BalanceStore {
  balanceData: BalanceData | null;
  setBalanceData: (data: BalanceData) => void;
  updateCoinBalance: (
    currency: string,
    balance: number,
    frozenBalance: number,
  ) => void;
  updateFiatBalance: (
    currency: string,
    balance: number,
    frozenBalance: number,
  ) => void;
  balanceUpdated: boolean;
  setBalanceUpdated: (value: boolean) => void;
}

export const useBalanceStore = create<BalanceStore>((set) => ({
  balanceData: null,
  setBalanceData: (data) => set({ balanceData: data }),
  updateCoinBalance: (currency, balance, frozenBalance) =>
    set((state) => {
      if (!state.balanceData) return { balanceData: null };

      const coinAccounts = [...state.balanceData.coin_accounts];
      const accountIndex = coinAccounts.findIndex(
        (a) => a.coin_currency === currency,
      );

      if (accountIndex >= 0) {
        coinAccounts[accountIndex] = {
          ...coinAccounts[accountIndex],
          balance,
          frozen_balance: frozenBalance,
        };
      } else {
        coinAccounts.push({
          coin_currency: currency,
          balance,
          frozen_balance: frozenBalance,
        } as CoinAccount);
      }

      return {
        balanceData: {
          ...state.balanceData,
          coin_accounts: coinAccounts,
        },
        balanceUpdated: true,
      };
    }),
  updateFiatBalance: (currency, balance, frozenBalance) =>
    set((state) => {
      if (!state.balanceData) return { balanceData: null };

      const fiatAccounts = [...state.balanceData.fiat_accounts];
      const accountIndex = fiatAccounts.findIndex(
        (a) => a.currency === currency,
      );

      if (accountIndex >= 0) {
        fiatAccounts[accountIndex] = {
          ...fiatAccounts[accountIndex],
          balance,
          frozen_balance: frozenBalance,
        };
      } else {
        fiatAccounts.push({
          currency,
          balance,
          frozen_balance: frozenBalance,
        } as FiatAccount);
      }

      return {
        balanceData: {
          ...state.balanceData,
          fiat_accounts: fiatAccounts,
        },
        balanceUpdated: true,
      };
    }),
  balanceUpdated: false,
  setBalanceUpdated: (value) => set({ balanceUpdated: value }),
}));
