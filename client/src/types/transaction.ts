export type TransactionType =
  | "deposit"
  | "withdrawal"
  | "fiat_deposit"
  | "fiat_withdrawal"
  | "buy"
  | "sell";

export type TransactionStatus =
  | "pending"
  | "completed"
  | "failed"
  | "cancelled"
  | "processing"
  | "verified";

export interface BaseTransaction {
  id: string;
  type: TransactionType;
  amount: number;
  status: TransactionStatus;
  created_at: string;
  updated_at: string;
  reference?: string;
}

export interface CryptoTransaction extends BaseTransaction {
  type: "deposit" | "withdrawal";
  coin_currency: string;
  hash: string;
  address?: string;
}

export interface FiatTransaction extends BaseTransaction {
  type: "fiat_deposit" | "fiat_withdrawal";
  currency: string;
  bank_account_id?: string;
  trade_id?: string;
}

export interface TradeTransaction extends BaseTransaction {
  type: "buy" | "sell";
  coin_currency: string;
  fiat_currency: string;
  price: number;
}

export type Transaction =
  | CryptoTransaction
  | FiatTransaction
  | TradeTransaction;

// Helper type guards
export const isCryptoTransaction = (tx: Transaction): tx is CryptoTransaction =>
  tx.type === "deposit" || tx.type === "withdrawal";

export const isFiatTransaction = (tx: Transaction): tx is FiatTransaction =>
  tx.type === "fiat_deposit" || tx.type === "fiat_withdrawal";

export const isTradeTransaction = (tx: Transaction): tx is TradeTransaction =>
  tx.type === "buy" || tx.type === "sell";

// Status constants
export const TRANSACTION_STATUS = Object.freeze({
  PENDING: "pending",
  COMPLETED: "completed",
  FAILED: "failed",
  CANCELLED: "cancelled",
  PROCESSING: "processing",
  VERIFIED: "verified",
});

// Type constants
export const TRANSACTION_TYPE = Object.freeze({
  DEPOSIT: "deposit",
  WITHDRAWAL: "withdrawal",
  FIAT_DEPOSIT: "fiat_deposit",
  FIAT_WITHDRAWAL: "fiat_withdrawal",
  BUY: "buy",
  SELL: "sell",
});
