import { useQuery } from "@tanstack/react-query";
import { CoinTransactionsResponse } from "@/types/coin-transactions";
import { fetchCoinTransactions } from "@/lib/api/coin-transactions";

interface UseCoinTransactionsParams {
  coin_currency: string;
  page?: number;
  per_page?: number;
}

export function useCoinTransactions({
  coin_currency,
  page = 1,
  per_page = 20,
}: UseCoinTransactionsParams) {
  return useQuery<CoinTransactionsResponse>({
    queryKey: ["coinTransactions", coin_currency, page, per_page],
    queryFn: () => fetchCoinTransactions({ coin_currency, page, per_page }),
  });
}
