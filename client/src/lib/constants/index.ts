import { Network } from "../types";
export const NETWORKS: Network[] = ["ERC20", "TRC20", "BEP20"];

export const FIAT_CURRENCIES = ["VND", "PHP", "NGN"] as const;

export const TRADE_EXPIRY_MINUTES = 30;

export const FIAT_CURRENCY_SYMBOLS = {
  VND: "₫",
  PHP: "₱",
} as const;

export const NETWORK_LABELS = {
  ERC20: "Ethereum (ERC20)",
  TRC20: "Tron (TRC20)",
  BEP20: "BNB Smart Chain (BEP20)",
} as const;

export const MOCK_RATES = {
  VND: {
    BUY: 23500,
    SELL: 24000,
  },
  PHP: {
    BUY: 55.5,
    SELL: 56.5,
  },
} as const;

export const MOCK_AMOUNT_LIMITS = {
  MIN: 50000,
  MAX: 1000000000,
} as const;

export const MAX_AMOUNT_PER_TRANSACTION = 300000000; // 300 million

export const DEPOSIT_FEES = {
  PERCENTAGE: 0.1, // 0.1%
  FIXED: {
    VND: 5000,
    PHP: 10, // Example fixed fee for PHP
    NGN: 200, // Example fixed fee for NGN
  },
} as const;
