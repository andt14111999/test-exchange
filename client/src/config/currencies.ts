export interface CurrencyInfo {
  name: string;
  symbol: string;
  type: "crypto" | "fiat";
  icon?: string;
}

export const CURRENCIES: Record<string, CurrencyInfo> = {
  // Cryptocurrencies
  BTC: {
    name: "Bitcoin",
    symbol: "BTC",
    type: "crypto",
    icon: "₿",
  },
  ETH: {
    name: "Ethereum",
    symbol: "ETH",
    type: "crypto",
    icon: "Ξ",
  },
  BNB: {
    name: "BNB",
    symbol: "BNB",
    type: "crypto",
    icon: "BNB",
  },
  USDT: {
    name: "Tether",
    symbol: "USDT",
    type: "crypto",
    icon: "USDT",
  },
  VND: {
    name: "Vietnamese Dong",
    symbol: "VND",
    type: "fiat",
    icon: "₫",
  },
  PHP: {
    name: "Philippine Peso",
    symbol: "PHP",
    type: "fiat",
    icon: "₱",
  },
};
