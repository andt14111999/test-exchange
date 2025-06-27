"use client";

import { CreateWalletForm } from "@/components/forms/create-wallet-form";
import { ProtectedLayout } from "@/components/protected-layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { WalletList } from "@/components/wallet-list";
import { useWallet } from "@/hooks/use-wallet";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { fetchCoinSettings, CoinSetting } from "@/lib/api/coins";

export default function WalletPage() {
  const t = useTranslations();
  const { data: walletData, isLoading, error } = useWallet();
  const [coinSettings, setCoinSettings] = useState<CoinSetting[] | null>(null);
  const [settingsLoading, setSettingsLoading] = useState(true);
  const [settingsError, setSettingsError] = useState<string | null>(null);

  useEffect(() => {
    fetchCoinSettings()
      .then((data) => {
        setCoinSettings(data);
        setSettingsLoading(false);
      })
      .catch((err) => {
        setSettingsError(
          err instanceof Error ? err.message : "Failed to load coin settings",
        );
        setSettingsLoading(false);
      });
  }, []);

  const loadingContent = (
    <div className="container py-8">
      <Card>
        <CardHeader>
          <CardTitle>
            <Skeleton className="h-8 w-48" />
          </CardTitle>
          <div className="text-sm text-muted-foreground">
            <Skeleton className="h-4 w-64" />
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        </CardContent>
      </Card>
    </div>
  );

  const content = (
    <div className="container py-8 space-y-8">
      <Card>
        <CardHeader>
          <CardTitle>{t("wallet.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading || settingsLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : error || settingsError ? (
            <div className="text-center p-4">
              <p className="text-destructive">
                {error instanceof Error
                  ? error.message
                  : settingsError || t("common.errors.failedToLoad")}
              </p>
            </div>
          ) : walletData && coinSettings ? (
            <div className="space-y-8">
              {walletData.coin_accounts.length > 0 && (
                <div className="space-y-4">
                  <h2 className="text-lg font-semibold">
                    {t("wallet.coinAccounts.title")}
                  </h2>
                  <WalletList
                    accounts={walletData.coin_accounts}
                    type="crypto"
                    coinSettings={coinSettings}
                  />
                </div>
              )}

              {walletData.fiat_accounts.length > 0 && (
                <div className="space-y-4">
                  <h2 className="text-lg font-semibold">
                    {t("wallet.fiatAccounts.title")}
                  </h2>
                  <WalletList accounts={walletData.fiat_accounts} type="fiat" />
                </div>
              )}

              {walletData.coin_accounts.length === 0 &&
                walletData.fiat_accounts.length === 0 && <CreateWalletForm />}
            </div>
          ) : (
            <CreateWalletForm />
          )}
        </CardContent>
      </Card>
    </div>
  );

  return (
    <ProtectedLayout loadingFallback={loadingContent}>
      {content}
    </ProtectedLayout>
  );
}
