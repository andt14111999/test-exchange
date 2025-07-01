export interface Token {
  id: string;
  symbol: string;
  name: string;
  decimals: number;
}

export const VND: Token = {
  id: "vnd",
  symbol: "₫",
  name: "Vietnamese Dong",
  decimals: 0,
};

export const USDT: Token = {
  id: "usdt",
  symbol: "$",
  name: "USD Tether",
  decimals: 6,
};

export const PHP: Token = {
  id: "php",
  symbol: "₱",
  name: "Philippine Peso",
  decimals: 2,
};

export const NGN: Token = {
  id: "ngn",
  symbol: "₦",
  name: "Nigerian Naira",
  decimals: 2,
};

/**
 * Get decimal places for a token from constants
 * @param tokenSymbol - Token symbol (case insensitive)
 * @returns Number of decimal places for the token
 */
export function getTokenDecimals(tokenSymbol: string): number {
  const tokens = [VND, USDT, PHP, NGN];
  const token = tokens.find(
    (t) =>
      t.id.toLowerCase() === tokenSymbol.toLowerCase() ||
      t.symbol === tokenSymbol ||
      t.name.toLowerCase().includes(tokenSymbol.toLowerCase()),
  );

  return token?.decimals ?? 6; // Default to 6 if token not found
}
