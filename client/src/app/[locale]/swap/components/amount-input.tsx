"use client";

import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { TokenSelect } from "./token-select";
import { useTranslations } from "next-intl";
import { NumberInputWithCommas } from "@/components/ui/number-input-with-commas";

interface AmountInputProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  token: string;
  onTokenChange: (value: string) => void;
  tokens: string[];
  placeholder?: string;
  tokenPlaceholder?: string;
  disabled?: boolean;
  error?: string;
  tokenBalance?: string;
  onMaxClick?: () => void;
}

export function AmountInput({
  label,
  value,
  onChange,
  token,
  onTokenChange,
  tokens,
  placeholder = "0.0",
  tokenPlaceholder = "Select token",
  disabled = false,
  error,
  tokenBalance = "0",
  onMaxClick,
}: AmountInputProps) {
  const t = useTranslations("swap");

  // Format số dư để hiển thị
  const formattedBalance = tokenBalance
    ? new Intl.NumberFormat("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        useGrouping: true,
      }).format(parseFloat(tokenBalance.replace(/,/g, "")))
    : "0";

  // Kiểm tra xem có thể hiển thị nút Max không
  const showMaxButton =
    onMaxClick &&
    tokenBalance &&
    parseFloat(tokenBalance.replace(/,/g, "")) > 0;

  return (
    <div className="space-y-2">
      <div className="flex justify-between items-center">
        <Label>{label}</Label>
        {token && (
          <div className="text-sm text-muted-foreground">
            <span>
              {t("balance")}: {formattedBalance}
            </span>
          </div>
        )}
      </div>
      <div className="flex space-x-2">
        <div className="relative flex-1">
          <NumberInputWithCommas
            value={value}
            onChange={onChange}
            placeholder={placeholder}
            disabled={disabled}
            data-testid="amount-input"
            className={showMaxButton ? "pr-16" : ""}
          />
          {showMaxButton && (
            <Button
              variant="ghost"
              size="sm"
              className="absolute right-2 top-1/2 -translate-y-1/2 h-6 px-2 text-xs font-semibold text-primary hover:text-primary hover:bg-primary/10 border border-primary/30 hover:border-primary"
              onClick={onMaxClick}
              disabled={disabled}
            >
              MAX
            </Button>
          )}
        </div>
        <div className="w-[120px] flex-shrink-0">
          <TokenSelect
            value={token}
            onValueChange={onTokenChange}
            tokens={tokens}
            placeholder={tokenPlaceholder}
            disabled={disabled}
          />
        </div>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
