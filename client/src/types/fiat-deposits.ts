export type FiatDepositStatus =
  | "awaiting"
  | "pending"
  | "money_sent"
  | "ownership_verifying"
  | "processing"
  | "processed"
  | "cancelled";

export interface FiatDeposit {
  id: string;
  currency: string;
  country_code: string;
  fiat_amount: number;
  deposit_fee: number;
  amount_after_fee: number;
  memo?: string;
  status: FiatDepositStatus;
  created_at: string;
  user_id: string;
  fiat_account_id: string;
  requires_ownership_verification: boolean;
  payable_id?: string;
  payable_type?: string;
  cancel_reason?: string;
  payment_proof_url?: string;
  payment_description?: string;
}

export interface CreateFiatDepositParams {
  currency: string;
  country_code: string;
  fiat_amount: number;
  fiat_account_id?: string;
  memo?: string;
  offer_id?: string;
  trade_id?: string;
  payment_method_id?: number;
  payment_proof_url?: string;
  payment_description?: string;
}

export interface OwnershipVerificationParams {
  sender_name: string;
  sender_account_number: string;
}

export interface FiatAccount {
  id: string;
  bank_name: string;
  account_number: string;
  account_holder_name: string;
  currency: string;
  is_default: boolean;
}

export interface MerchantOffer {
  id: string;
  merchantName: string;
  rate: number;
  minAmount: number;
  maxAmount: number;
  accountInfo?: {
    bank_name: string;
    account_number: string;
    account_holder_name: string;
  };
}
