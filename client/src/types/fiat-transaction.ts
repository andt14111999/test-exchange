export interface FiatTransaction {
  id: string;
  fiat_amount: number;
  currency: string;
  status: string;
  created_at: string;
  coin_currency: string;
  hash: string;
  updated_at: string;
  trade_id?: number;
}
