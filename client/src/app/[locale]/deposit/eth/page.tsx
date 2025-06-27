"use client";

import { useTranslations } from "next-intl";
import { CoinDeposit } from "@/components/deposit/coin-deposit";

const networks = [{ id: "erc20", name: "Ethereum (ERC20)", enabled: true }];

export default function DepositETHPage() {
  const t = useTranslations("deposit");

  return (
    <CoinDeposit
      coin="eth"
      networks={networks}
      title={t("eth.title")}
      description={t("eth.description")}
    />
  );
}
