import { apiClient } from "./client";

export interface TradingFees {
  fee_ratios: {
    vnd: number;
    php: number;
    ngn: number;
    default: number;
    [key: string]: number;
  };
  fixed_fees: {
    vnd: number;
    php: number;
    ngn: number;
    default: number;
    [key: string]: number;
  };
}

export interface TradingFeesResponse {
  trading_fees: TradingFees;
}

export interface ExchangeRates {
  usdt_to_vnd: number;
  usdt_to_php: number;
  usdt_to_ngn: number;
}

export interface WithdrawalFees {
  usdt_erc20: number;
  usdt_bep20: number;
  usdt_trc20: number;
  usdt_solana: number;
  [key: string]: number; // For any other fees that might be added in the future
}

export async function getExchangeRates() {
  const response = await apiClient.get<{ exchange_rates: ExchangeRates }>(
    "/settings/exchange_rates",
  );
  return response.data.exchange_rates;
}

export async function getWithdrawalFees() {
  const response = await apiClient.get<{ withdrawal_fees: WithdrawalFees }>(
    "/settings/withdrawal_fees",
  );
  return response.data.withdrawal_fees;
}

export const getTradingFees = async (): Promise<TradingFees> => {
  const response = await apiClient.get<TradingFeesResponse>(
    "/settings/trading_fees",
  );
  return response.data.trading_fees;
};
