import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../client";
import { API_ENDPOINTS } from "../config";
import {
  Transaction,
  TransactionType,
  TransactionStatus,
} from "@/types/transaction";

interface TransactionsResponse {
  data: Transaction[];
  total_pages: number;
  current_page: number;
  total_count: number;
}

interface UseTransactionsParams {
  page?: number;
  per_page?: number;
  type?: TransactionType;
  status?: TransactionStatus;
  start_date?: string;
  end_date?: string;
}

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_PAGE = 1;
const STALE_TIME = 1000 * 60; // 1 minute
const MAX_RETRIES = 3;
const MAX_RETRY_DELAY = 30000; // 30 seconds

export function useTransactions({
  page = DEFAULT_PAGE,
  per_page = DEFAULT_PAGE_SIZE,
  type,
  status,
  start_date,
  end_date,
}: UseTransactionsParams = {}) {
  return useQuery({
    queryKey: [
      "transactions",
      page,
      per_page,
      type,
      status,
      start_date,
      end_date,
    ],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: page.toString(),
        per_page: per_page.toString(),
        ...(type && { type }),
        ...(status && { status }),
        ...(start_date && { start_date }),
        ...(end_date && { end_date }),
      });

      const response = await apiClient.get<TransactionsResponse>(
        `${API_ENDPOINTS.transactions.list}?${params.toString()}`,
      );
      return response.data;
    },
    staleTime: STALE_TIME,
    retry: MAX_RETRIES,
    retryDelay: (attemptIndex) =>
      Math.min(1000 * 2 ** attemptIndex, MAX_RETRY_DELAY),
  });
}
