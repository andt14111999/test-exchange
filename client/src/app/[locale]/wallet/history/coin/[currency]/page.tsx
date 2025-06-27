"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import { TransactionHistoryTabs } from "@/components/transaction-history-tabs";
import { useCoinTransactions } from "@/hooks/use-coin-transactions";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useParams } from "next/navigation";
import { Transaction } from "@/types/transaction";
import { useEffect, useState } from "react";
import { getFiatDeposits } from "@/lib/api/fiat-deposits";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/use-toast";
import axios from "axios";
import { Loader2 } from "lucide-react";

// Define the structure for fiat deposit history items
interface FiatDeposit {
  id: string;
  fiat_amount: number;
  currency: string;
  status: string;
  created_at: string;
  coin_currency: string;
  hash: string;
  updated_at: string;
}

// Function to convert FiatDeposit to Transaction type
const convertFiatDepositToTransaction = (deposit: FiatDeposit): Transaction => {
  return {
    ...deposit,
    amount: deposit.fiat_amount,
    type: "fiat_deposit" as const,
    coin_currency: deposit.currency,
    hash: "",
    updated_at: deposit.created_at,
    id: deposit.id,
  } as Transaction;
};

export default function TransactionHistoryPage() {
  const t = useTranslations();
  const params = useParams();
  const { toast } = useToast();
  const currency = params.currency as string;

  // State for fiat deposit history
  const [depositHistory, setDepositHistory] = useState<FiatDeposit[]>([]);
  const [isLoadingFiatHistory, setIsLoadingFiatHistory] = useState(false);

  const {
    data: transactionsData,
    isLoading: isLoadingCoinTransactions,
    error: coinTransactionsError,
  } = useCoinTransactions({ coin_currency: currency });

  // Fetch deposit history when component mounts
  useEffect(() => {
    if (currency.toUpperCase() === "VND" || currency.toUpperCase() === "PHP") {
      fetchDepositHistory();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currency]);

  // Fetch deposit history
  const fetchDepositHistory = async () => {
    setIsLoadingFiatHistory(true);
    try {
      const response = await getFiatDeposits();

      // Filter deposits for current currency
      const filteredDeposits = Array.isArray(response)
        ? response.filter(
            (deposit) =>
              deposit.currency.toLowerCase() === currency.toLowerCase(),
          )
        : [];

      setDepositHistory(filteredDeposits);
    } catch (error) {
      console.error("Error fetching deposit history:", error);
      toast({
        title: t("deposit.errorFetchingHistory", {
          fallback: "Failed to load deposit history",
        }),
        description: axios.isAxiosError(error)
          ? error.response?.data?.message
          : t("deposit.somethingWentWrong", {
              fallback: "Something went wrong",
            }),
        variant: "destructive",
      });
    } finally {
      setIsLoadingFiatHistory(false);
    }
  };

  // Helper function to get status class for deposit
  const getStatusClass = (status: string) => {
    switch (status.toLowerCase()) {
      case "completed":
      case "processed":
      case "verified":
        return "bg-green-100 text-green-800";
      case "cancelled":
        return "bg-red-100 text-red-800";
      case "pending":
      case "awaiting":
      case "money_sent":
        return "bg-yellow-100 text-yellow-800";
      case "processing":
        return "bg-yellow-50 text-yellow-700";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  // Combine coin transactions with fiat deposits
  const coinTransactions = [
    ...(transactionsData?.data.deposits || []).map((tx) => ({
      ...tx,
      type: "deposit" as const,
      id: tx.id.toString(),
    })),
    ...(transactionsData?.data.withdrawals || []).map((tx) => ({
      ...tx,
      type: "withdrawal" as const,
      id: tx.id.toString(),
    })),
  ] as Transaction[];

  // Convert fiat deposits to transaction format and add them
  const fiatDeposits = depositHistory.map(convertFiatDepositToTransaction);

  // Combine all transactions and sort by date
  const allTransactions = [...coinTransactions, ...fiatDeposits].sort(
    (a, b) =>
      new Date(b.created_at).getTime() - new Date(a.created_at).getTime(),
  );

  const isLoading = isLoadingCoinTransactions || isLoadingFiatHistory;

  return (
    <ProtectedLayout>
      <div className="container py-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>
              {t("wallet.history.title")} - {currency.toUpperCase()}
            </CardTitle>
            {(currency.toUpperCase() === "VND" ||
              currency.toUpperCase() === "PHP") && (
              <Button
                variant="outline"
                size="sm"
                onClick={fetchDepositHistory}
                disabled={isLoadingFiatHistory}
              >
                {isLoadingFiatHistory ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                {t("deposit.refreshHistory", { fallback: "Refresh History" })}
              </Button>
            )}
          </CardHeader>
          <CardContent>
            {coinTransactionsError ? (
              <div className="text-center p-4">
                <p className="text-destructive">
                  {t("common.errors.failedToLoad")}
                </p>
              </div>
            ) : (
              <TransactionHistoryTabs
                transactions={allTransactions}
                isLoading={isLoading}
                statusClassFn={getStatusClass}
              />
            )}
          </CardContent>
        </Card>
      </div>
    </ProtectedLayout>
  );
}
