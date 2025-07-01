"use client";

import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { formatNumber } from "@/lib/utils/index";
import { useTranslations } from "next-intl";
import { MINIMUM_WITHDRAWAL_USDT } from "@/lib/constants/withdrawal";

interface InternalWithdrawTabProps {
  username: string;
  onUsernameChange: (username: string) => void;
  usernameError: string | null;
  isValidatingUsername: boolean;
  amount: string;
  onAmountChange: (amount: string) => void;
  amountError: string | null;
  usdtBalance: number;
  isLoadingWallet: boolean;
  userHas2FA: boolean;
  isDeviceTrusted: boolean;
  isCheckingDevice: boolean;
}

export function InternalWithdrawTab({
  username,
  onUsernameChange,
  usernameError,
  isValidatingUsername,
  amount,
  onAmountChange,
  amountError,
  usdtBalance,
  isLoadingWallet,
  userHas2FA,
  isDeviceTrusted,
  isCheckingDevice,
}: InternalWithdrawTabProps) {
  const t = useTranslations("withdraw.usdt");
  const tWithdraw = useTranslations("withdraw");

  const parsedAmount = parseFloat(amount) || 0;

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="username" className="text-sm font-medium">
          {t("recipientUsername")}
        </Label>
        <Input
          id="username"
          placeholder={t("enterUsername")}
          value={username}
          onChange={(e) => onUsernameChange(e.target.value)}
          className={usernameError ? "border-red-500" : ""}
        />
        <div className="text-xs text-muted-foreground">
          {t("usernameExample")}
        </div>
        {usernameError && (
          <div className="text-sm text-red-600">{usernameError}</div>
        )}
        {isValidatingUsername && (
          <div className="text-sm text-blue-600">{t("validatingUsername")}</div>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="amount-internal" className="text-sm font-medium">
          {t("amount")}
        </Label>
        <Input
          id="amount-internal"
          type="number"
          placeholder={t("enterAmount")}
          value={amount}
          onChange={(e) => onAmountChange(e.target.value)}
          className={amountError ? "border-red-500" : ""}
        />
        <div className="text-sm text-muted-foreground">
          {t("availableBalance", {
            balance: isLoadingWallet ? "Loading..." : formatNumber(usdtBalance),
          })}
        </div>
        {parsedAmount > 0 && (
          <div className="text-sm">
            {t("totalAmount", {
              amount: formatNumber(parsedAmount),
            })}
          </div>
        )}
        {amountError && (
          <div className="text-sm text-red-600">{amountError}</div>
        )}
      </div>

      <div className="text-sm text-muted-foreground">
        <p>Important:</p>
        <ul className="list-disc list-inside space-y-1">
          <li>{t("minTransfer", { amount: MINIMUM_WITHDRAWAL_USDT })}</li>
          <li>{t("noNetworkFees")}</li>
          <li>{t("instantTransfers")}</li>
          {!isCheckingDevice && !(userHas2FA && isDeviceTrusted) && (
            <li className="text-orange-600 font-medium">
              {tWithdraw("twoFactorRequired")}
            </li>
          )}
        </ul>
      </div>
    </div>
  );
}
