"use client";

import { useTranslations } from "next-intl";
import { CoinDeposit } from "@/components/deposit/coin-deposit";
import { useCoinNetworks } from "@/hooks/use-coin-networks";

export default function DepositBNBPage() {
  const t = useTranslations("deposit");
  const { networks, isLoading } = useCoinNetworks("bnb");

  if (isLoading) {
    return null; // or a loading spinner
  }

  return (
    <CoinDeposit
      coin="bnb"
      networks={networks}
      title={t("bnb.title")}
      description={t("bnb.description")}
    />
  );
}
