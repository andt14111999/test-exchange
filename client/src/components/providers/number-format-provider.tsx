"use client";

import { createContext, useContext, useEffect, useState } from "react";
import BigNumber from "bignumber.js";

type NumberFormatContextType = {
  isHydrated: boolean;
  locale: string;
  formatNumber: (
    value: number | string | BigNumber,
    options?: {
      decimals?: number;
      currency?: string;
      showSymbol?: boolean;
    },
  ) => string;
};

const CURRENCY_SYMBOLS: Record<string, string> = {
  VND: "₫",
  PHP: "₱",
  NGN: "₦",
  USDT: "$",
};

const NumberFormatContext = createContext<NumberFormatContextType>({
  isHydrated: false,
  locale: "en-US",
  formatNumber: () => "0",
});

export function useNumberFormat() {
  return useContext(NumberFormatContext);
}

export function NumberFormatProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [isHydrated, setIsHydrated] = useState(false);
  const [locale, setLocale] = useState("en-US");

  useEffect(() => {
    setIsHydrated(true);
    setLocale(navigator.language);
  }, []);

  const formatNumber = (
    value: number | string | BigNumber,
    options: {
      decimals?: number;
      currency?: string;
      showSymbol?: boolean;
    } = {},
  ) => {
    const { decimals = 2, currency, showSymbol = true } = options;

    if (!isHydrated) {
      // SSR-safe format
      const symbol =
        currency && showSymbol ? CURRENCY_SYMBOLS[currency] || "" : "";
      return currency ? `${symbol}${value}` : value.toString();
    }

    try {
      const bn = new BigNumber(value);
      const numberFormat = new Intl.NumberFormat(locale, {
        minimumFractionDigits: currency ? 0 : decimals,
        maximumFractionDigits: currency ? 0 : decimals,
      });

      const formattedNumber = numberFormat.format(bn.toNumber());
      const symbol =
        currency && showSymbol ? CURRENCY_SYMBOLS[currency] || "" : "";

      return `${symbol}${formattedNumber}`;
    } catch {
      return "0";
    }
  };

  return (
    <NumberFormatContext.Provider value={{ isHydrated, locale, formatNumber }}>
      {children}
    </NumberFormatContext.Provider>
  );
}
