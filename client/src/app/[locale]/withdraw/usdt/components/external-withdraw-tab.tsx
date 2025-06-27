"use client";

import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { formatNumber } from "@/lib/utils/index";
import { useTranslations } from "next-intl";
import { Network } from "./types";

interface ExternalWithdrawTabProps {
  networks: Network[];
  selectedNetwork: Network | null;
  onNetworkChange: (network: Network | null) => void;
  address: string;
  onAddressChange: (address: string) => void;
  addressError: string | null;
  amount: string;
  onAmountChange: (amount: string) => void;
  amountError: string | null;
  usdtBalance: number;
  isLoadingWallet: boolean;
  isLoadingNetworks: boolean;
  userHas2FA: boolean;
  isDeviceTrusted: boolean;
  isCheckingDevice: boolean;
}

export function ExternalWithdrawTab({
  networks,
  selectedNetwork,
  onNetworkChange,
  address,
  onAddressChange,
  addressError,
  amount,
  onAmountChange,
  amountError,
  usdtBalance,
  isLoadingWallet,
  isLoadingNetworks,
  userHas2FA,
  isDeviceTrusted,
  isCheckingDevice,
}: ExternalWithdrawTabProps) {
  const t = useTranslations("withdraw.usdt");
  const tWithdraw = useTranslations("withdraw");

  const parsedAmount = parseFloat(amount) || 0;

  const getAddressExample = (networkId: string) => {
    switch (networkId) {
      case "bep20":
        return " 0x71C7656EC7ab88b098defB751B7401B5f6d8976F";
      case "trc20":
        return " TKQpQkMWRvTJpQgYrGp8wKgJSHV3DqNHJ3";
      case "erc20":
        return " 0x71C7656EC7ab88b098defB751B7401B5f6d8976F";
      case "solana":
        return " 7UX2i7SucgLMQcfZ75s3VXmZZY4YRUyJN9X1RgfMoDUi";
      default:
        return "";
    }
  };

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="network" className="text-sm font-medium">
          {t("network")}
        </Label>
        <Select
          value={selectedNetwork?.id}
          onValueChange={(value) => {
            const network = networks.find((n) => n.id === value);
            if (network && network.enabled) {
              onNetworkChange(network);
            }
          }}
          disabled={isLoadingNetworks}
        >
          <SelectTrigger>
            <SelectValue placeholder={t("selectNetwork")} />
          </SelectTrigger>
          <SelectContent>
            {networks.map((network) => (
              <SelectItem
                key={network.id}
                value={network.id}
                disabled={!network.enabled}
                className={!network.enabled ? "text-muted-foreground" : ""}
              >
                {network.name}
                {!network.enabled && (
                  <>
                    {" "}
                    <span className="text-muted-foreground">
                      {t("networkDisabled")}
                    </span>
                  </>
                )}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor="address" className="text-sm font-medium">
          {t("destinationAddress")}
        </Label>
        <Input
          placeholder={
            selectedNetwork
              ? t("enterAddress", { network: selectedNetwork.name })
              : t("selectNetworkFirst")
          }
          value={address}
          onChange={(e) => onAddressChange(e.target.value)}
          className={addressError ? "border-red-500" : ""}
        />
        <div className="text-xs text-muted-foreground">
          Example:
          {selectedNetwork && getAddressExample(selectedNetwork.id)}
        </div>
        {addressError && (
          <div className="text-sm text-red-600">{addressError}</div>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="amount" className="text-sm font-medium">
          {t("amount")}
        </Label>
        <Input
          id="amount"
          type="number"
          placeholder={t("enterAmount")}
          value={amount}
          onChange={(e) => onAmountChange(e.target.value)}
          className={amountError ? "border-red-500" : ""}
        />
        <div className="text-sm text-muted-foreground">
          {t("networkFee", { fee: selectedNetwork?.fee || 0 })}
        </div>
        <div className="text-sm text-muted-foreground">
          {t("availableBalance", {
            balance: isLoadingWallet ? "Loading..." : formatNumber(usdtBalance),
          })}
        </div>
        {parsedAmount > 0 && (
          <div className="text-sm">
            {t("totalAmount", {
              amount: formatNumber(parsedAmount + (selectedNetwork?.fee || 0)),
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
          <li>{t("minWithdrawal")}</li>
          <li>{t("networkFee", { fee: selectedNetwork?.fee || 0 })}</li>
          <li>
            {t("onlyWithdrawToNetwork", {
              network: selectedNetwork?.name || "",
            })}
          </li>
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
