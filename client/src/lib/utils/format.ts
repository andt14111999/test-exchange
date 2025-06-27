import { BigNumber } from "bignumber.js";

export function formatCurrency(
  amount: string | number | BigNumber,
  currency: string = "USD",
  options: {
    decimals?: number;
    showSymbol?: boolean;
  } = {},
) {
  const { decimals = 2, showSymbol = true } = options;

  try {
    const bn = new BigNumber(amount);
    const numberFormat = new Intl.NumberFormat("en-US", {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    });
    const value = numberFormat.format(bn.toNumber());
    const symbols: Record<string, string> = {
      VND: "₫",
      PHP: "₱",
      NGN: "₦",
      USDT: "$",
      USD: "$",
    };
    const symbol = symbols[currency] || "";

    return showSymbol ? `${symbol}${value}` : value;
  } catch {
    return "0";
  }
}

export function formatFiatAmount(amount: number, currency: string) {
  // Luôn sử dụng en-US locale cho server-side rendering để tránh hydration mismatch
  const formatter = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });

  const symbols: Record<string, string> = {
    VND: "₫",
    PHP: "₱",
    NGN: "₦",
    USDT: "$",
  };

  const symbol = symbols[currency] || "";
  return `${symbol}${formatter.format(amount)}`;
}

/**
 * Định dạng giá trị thanh khoản với 2 chữ số thập phân
 */
export function formatLiquidity(value: string | number) {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value));
}

export function formatDate(date: Date): string {
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
