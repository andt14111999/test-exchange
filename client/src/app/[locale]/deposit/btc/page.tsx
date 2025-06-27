"use client";

import { useTranslations } from "next-intl";
import { CoinDeposit } from "@/components/deposit/coin-deposit";
import { useCoinNetworks } from "@/hooks/use-coin-networks";

export default function DepositBTCPage() {
  const t = useTranslations("deposit");
  const { networks, isLoading } = useCoinNetworks("btc");

  if (isLoading) {
    return null; // or a loading spinner
  }

  return (
    <CoinDeposit
      coin="btc"
      networks={networks}
      title={t("btc.title")}
      description={t("btc.description")}
    />
  );
}
