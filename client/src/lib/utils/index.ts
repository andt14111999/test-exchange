import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatFiatAmount(amount: number, currency: string): string {
  const formatter = new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });
  return formatter.format(amount);
}

export function formatUSDTAmount(amount: number): string {
  const formatter = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  return `${formatter.format(amount)} USDT`;
}

export function formatWalletAddress(address: string): string {
  if (!address) return "";
  return `${address.slice(0, 6)}...${address.slice(-4)}`;
}

export function calculateTimeLeft(expiresAt: Date): number {
  const now = new Date();
  const diff = expiresAt.getTime() - now.getTime();
  return Math.max(0, Math.floor(diff / 1000));
}

export function generateMockId(): string {
  return Math.random().toString(36).substring(2, 15);
}

export function isValidAmount(
  amount: number,
  min: number,
  max: number,
): boolean {
  return amount >= min && amount <= max;
}

export function calculateFiatAmount(usdtAmount: number, rate: number): number {
  return usdtAmount * rate;
}

export function formatNumber(value: number): string {
  if (isNaN(value)) return "0";
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 8,
  }).format(value);
}
