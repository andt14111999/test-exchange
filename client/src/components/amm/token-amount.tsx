"use client";

import { BigNumber } from "bignumber.js";
import { useNumberFormat } from "@/components/providers/number-format-provider";

interface TokenAmountProps {
  amount: string | number | BigNumber;
  token: string;
  decimals?: number;
  className?: string;
  showSymbol?: boolean;
}

export function TokenAmount({
  amount,
  token,
  decimals = 2,
  className = "",
  showSymbol = true,
}: TokenAmountProps) {
  const { formatNumber } = useNumberFormat();

  const formatted = formatNumber(amount, {
    decimals,
    currency: token,
    showSymbol,
  });

  return (
    <span className={className}>
      {formatted} {token}
    </span>
  );
}
