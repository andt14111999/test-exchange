export interface Network {
  id: string;
  name: string;
  fee: number;
  enabled: boolean;
}

export interface CreateWithdrawalRequest {
  coin_amount: number;
  coin_currency: string;
  coin_layer: string;
  coin_address?: string;
  receiver_username?: string;
  two_factor_code?: string;
}

export const addressPatterns = {
  bep20: /^0x[a-fA-F0-9]{40}$/,
  trc20: /^T[a-zA-Z0-9]{33}$/,
  erc20: /^0x[a-fA-F0-9]{40}$/,
  solana: /^[1-9A-HJ-NP-Za-km-z]{32,44}$/,
} as const;
