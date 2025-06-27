"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useRouter } from "@/navigation";

export function CreateWalletForm() {
  const t = useTranslations();
  const router = useRouter();
  const [currency, setCurrency] = useState<string>("");

  const handleCreate = () => {
    // Here you would typically make an API call to create a wallet
    // For now, we'll just redirect to the wallet page
    router.push("/wallet");
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("wallet.createWallet.title")}</CardTitle>
        <CardDescription>
          {t("wallet.createWallet.description")}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">
              {t("wallet.createWallet.selectCurrency")}
            </label>
            <Select value={currency} onValueChange={setCurrency}>
              <SelectTrigger>
                <SelectValue
                  placeholder={t(
                    "wallet.createWallet.selectCurrencyPlaceholder",
                  )}
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="btc">Bitcoin (BTC)</SelectItem>
                <SelectItem value="eth">Ethereum (ETH)</SelectItem>
                <SelectItem value="usdt">Tether (USDT)</SelectItem>
                <SelectItem value="usdc">USD Coin (USDC)</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </CardContent>
      <CardFooter>
        <Button onClick={handleCreate} disabled={!currency}>
          {t("wallet.createWallet.createButton")}
        </Button>
      </CardFooter>
    </Card>
  );
}
