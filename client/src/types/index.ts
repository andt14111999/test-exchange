// User types
export interface User {
  id: number;
  email: string;
  display_name?: string;
  name?: string;
  avatar_url?: string;
  status: string;
  kyc_level: number;
  phone_verified: boolean;
  document_verified: boolean;
  severity: number;
  role?: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

// Wallet types
export interface CoinAccount {
  id: number;
  user_id: number;
  coin_currency: string;
  balance: number;
  frozen_balance?: number;
  created_at: string;
  updated_at: string;
}

export interface FiatAccount {
  id: number;
  user_id: number;
  currency: string;
  balance: number;
  frozen_balance?: number;
  created_at: string;
  updated_at: string;
}

export interface WalletData {
  coin_accounts: CoinAccount[];
  fiat_accounts: FiatAccount[];
}
