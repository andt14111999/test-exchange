export interface CoinTransaction {
  id: number;
  amount: number;
  coin_currency: string;
  status: string;
  hash: string;
  address?: string;
  created_at: string;
  updated_at: string;
}

export interface Pagination {
  current_page: number;
  total_pages: number;
  total_count: number;
  per_page: number;
}

export interface CoinTransactionsResponse {
  status: string;
  data: {
    deposits: CoinTransaction[];
    withdrawals: CoinTransaction[];
    pagination: {
      deposits: Pagination;
      withdrawals: Pagination;
    };
  };
}
