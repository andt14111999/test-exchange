"use client";

import { useTranslations } from "next-intl";
import { CoinDeposit } from "@/components/deposit/coin-deposit";
import { useCoinNetworks } from "@/hooks/use-coin-networks";

export default function DepositUSDTPage() {
  const t = useTranslations("deposit");
  const { networks, isLoading } = useCoinNetworks("usdt");

  if (isLoading) {
    return null; // or a loading spinner
  }

  return (
    <CoinDeposit
      coin="usdt"
      networks={networks}
      title={t("usdt.title")}
      description={t("usdt.description")}
    />
  );
}
