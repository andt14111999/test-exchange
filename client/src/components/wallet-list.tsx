"use client";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { CoinAccount, FiatAccount } from "@/types";
import { useRouter } from "@/navigation";
import { useTranslations } from "next-intl";
import { CURRENCIES } from "@/config/currencies";
import { CoinSetting } from "@/lib/api/coins";

type Account = CoinAccount | FiatAccount;

interface WalletListProps {
  accounts: Account[];
  type: "crypto" | "fiat";
  coinSettings?: CoinSetting[];
}

export function WalletList({ accounts, type, coinSettings }: WalletListProps) {
  const router = useRouter();
  const t = useTranslations();

  const handleDeposit = (token: string) => {
    router.push(`/deposit/${token.toLowerCase()}`);
  };

  const handleWithdraw = (token: string) => {
    router.push(`/withdraw/${token.toLowerCase()}`);
  };

  const handleSwap = (token: string) => {
    router.push(`/swap?token=${token}`);
  };

  const handleViewHistory = (token: string) => {
    if (type === "crypto") {
      router.push(`/wallet/history/coin/${token.toLowerCase()}`);
    } else {
      router.push(`/wallet/history/fiat/${token.toLowerCase()}`);
    }
  };

  const formatNumber = (num: number) => {
    return new Intl.NumberFormat("en-US", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 8,
    }).format(num);
  };

  // Helper function to get the currency from an account
  const getCurrency = (account: Account): string => {
    const currency =
      "coin_currency" in account ? account.coin_currency : account.currency;
    return currency;
  };

  const getCurrencyInfo = (symbol: string) => {
    return (
      CURRENCIES[symbol.toUpperCase()] || {
        name: symbol,
        symbol: symbol,
        type: "crypto",
        icon: symbol,
      }
    );
  };

  // Helper để lấy trạng thái coin/layer
  const getCoinSetting = (currency: string) => {
    if (!coinSettings || type === "fiat") return undefined;
    const setting = coinSettings.find(
      (c) => c.currency.toLowerCase() === currency.toLowerCase(),
    );
    return setting;
  };

  // Nếu chỉ có 1 layer thì coi như account thuộc layer đó
  const getLayerSetting = (coinSetting: CoinSetting) => {
    if (coinSetting?.layers && coinSetting.layers.length === 1) {
      return coinSetting.layers[0];
    }
    return undefined;
  };

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("wallet.token")}</TableHead>
            <TableHead>{t("wallet.balance")}</TableHead>
            <TableHead>{t("wallet.frozen")}</TableHead>
            <TableHead className="text-right">{t("wallet.actions")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {accounts.map((account, index) => {
            const currency = getCurrency(account);
            const frozenBalance = account.frozen_balance || 0;
            const currencyInfo = getCurrencyInfo(currency);
            const coinSetting = getCoinSetting(currency);
            const layerSetting = coinSetting
              ? getLayerSetting(coinSetting)
              : undefined;

            // Sửa lại logic: Nếu là fiat thì luôn enable deposit và withdraw, disable swap
            const isFiat = type === "fiat";
            const isLayerMaintenance =
              !isFiat && layerSetting ? layerSetting.maintenance : false;
            const isCoinMaintenance =
              !isFiat && !layerSetting && coinSetting?.layers?.length
                ? coinSetting.layers.every((l) => l.maintenance)
                : false;

            const depositEnabled = isFiat
              ? true
              : !isLayerMaintenance &&
                !isCoinMaintenance &&
                (layerSetting
                  ? layerSetting.deposit_enabled
                  : coinSetting?.deposit_enabled);
            const withdrawEnabled = isFiat
              ? true
              : !isLayerMaintenance &&
                !isCoinMaintenance &&
                (layerSetting
                  ? layerSetting.withdraw_enabled
                  : coinSetting?.withdraw_enabled);
            const swapEnabled = isFiat
              ? true
              : !isLayerMaintenance &&
                !isCoinMaintenance &&
                (layerSetting
                  ? layerSetting.swap_enabled
                  : coinSetting?.swap_enabled);
            const allDisabled =
              !isFiat && (isLayerMaintenance || isCoinMaintenance);

            // Tạo key duy nhất từ id, loại tài khoản và loại tiền tệ
            const uniqueKey = `${type}-${currency}-${account.id || index}`;

            return (
              <TableRow key={uniqueKey}>
                <TableCell className="font-medium">
                  <div className="flex items-center gap-3">
                    <span className="text-xl font-bold">
                      {currencyInfo.icon}
                    </span>
                    <div className="flex flex-col">
                      <span className="font-medium">{currencyInfo.name}</span>
                      <div className="flex items-center gap-2">
                        <Badge variant="secondary" className="w-fit">
                          {currencyInfo.symbol}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {type === "crypto"
                            ? t("wallet.crypto")
                            : t("wallet.fiat")}
                        </span>
                      </div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium">
                      {formatNumber(account.balance || 0)}
                    </span>
                    <span className="text-sm text-muted-foreground">
                      {currencyInfo.symbol}
                    </span>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium">
                      {formatNumber(frozenBalance)}
                    </span>
                    <span className="text-sm text-muted-foreground">
                      {currencyInfo.symbol}
                    </span>
                  </div>
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleDeposit(currency)}
                      disabled={!depositEnabled}
                    >
                      {t("wallet.deposit")}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleWithdraw(currency)}
                      disabled={!withdrawEnabled}
                    >
                      {t("wallet.withdraw")}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleSwap(currency)}
                      disabled={!swapEnabled}
                    >
                      {t("wallet.swap")}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleViewHistory(currency)}
                      disabled={allDisabled}
                    >
                      {t("wallet.viewHistory")}
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
