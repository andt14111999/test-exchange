"use client";

import { Button } from "@/components/ui/button";
import { BigNumber } from "bignumber.js";
import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { AlertCircle } from "lucide-react";

// Simple Alert component for error messages
const Alert = ({
  variant = "default",
  children,
  className = "",
}: {
  variant?: "default" | "destructive";
  children: React.ReactNode;
  className?: string;
}) => (
  <div
    className={`p-4 rounded-md border ${
      variant === "destructive"
        ? "bg-red-50 border-red-200 text-red-800"
        : "bg-blue-50 border-blue-200 text-blue-800"
    } ${className}`}
  >
    {children}
  </div>
);

const AlertDescription = ({ children }: { children: React.ReactNode }) => (
  <div className="text-sm">{children}</div>
);

export interface SwapSummaryProps {
  exchangeRate: BigNumber | null;
  outputToken: string;
  inputToken: string;
  disabled: boolean;
  onSwap: () => void;
  buttonText: string;
  errorMessage?: string;
  priceImpact: number;
}

export function SwapSummary({
  exchangeRate,
  outputToken,
  inputToken,
  disabled,
  onSwap,
  buttonText,
  errorMessage,
  priceImpact,
}: SwapSummaryProps) {
  const t = useTranslations("swap");

  // Format the exchange rate for display
  const formattedRate = useMemo(() => {
    if (!exchangeRate) return "0";

    try {
      const rate = exchangeRate.toNumber();
      return rate.toLocaleString("en-US", {
        maximumFractionDigits: rate > 1 ? 2 : 6,
        minimumFractionDigits: 0,
      });
    } catch {
      return "0";
    }
  }, [exchangeRate]);

  // Determine swap button color based on price impact
  const getButtonVariant = useMemo(() => {
    if (priceImpact >= 10) return "destructive";
    if (priceImpact >= 5) return "secondary";
    return "default";
  }, [priceImpact]);

  return (
    <div className="space-y-4">
      {exchangeRate && exchangeRate.gt(0) && (
        <div className="flex justify-between text-sm text-muted-foreground">
          <div>{t("exchangeRate")}</div>
          <div>
            1 {inputToken} = {formattedRate} {outputToken}
          </div>
        </div>
      )}

      {priceImpact > 0 && (
        <div className="flex justify-between text-sm">
          <div>{t("priceImpact")}</div>
          <div
            className={
              priceImpact >= 10
                ? "text-red-600"
                : priceImpact >= 5
                  ? "text-amber-600"
                  : "text-muted-foreground"
            }
          >
            {priceImpact > 0 && priceImpact < 0.01
              ? "< 0.01%"
              : priceImpact.toFixed(2) + "%"}
          </div>
        </div>
      )}

      {errorMessage && (
        <Alert variant="destructive" className="text-center">
          <div className="flex items-center justify-center gap-2">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{errorMessage}</AlertDescription>
          </div>
        </Alert>
      )}

      <Button
        onClick={onSwap}
        className="w-full"
        disabled={disabled}
        variant={getButtonVariant}
      >
        {buttonText}
      </Button>
    </div>
  );
}
