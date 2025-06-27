import { CoinTransactionsResponse } from "@/types/coin-transactions";
import { apiClient } from "./client";

interface GetCoinTransactionsParams {
  coin_currency: string;
  page?: number;
  per_page?: number;
}

export async function fetchCoinTransactions({
  coin_currency,
  page = 1,
  per_page = 20,
}: GetCoinTransactionsParams): Promise<CoinTransactionsResponse> {
  const response = await apiClient.get("/coin_transactions", {
    params: {
      coin_currency,
      page,
      per_page,
    },
  });
  return response.data;
}
