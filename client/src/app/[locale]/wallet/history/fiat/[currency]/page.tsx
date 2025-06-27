"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import { TransactionHistoryTabs } from "@/components/transaction-history-tabs";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getFiatDeposits, getFiatWithdrawals } from "@/lib/api/fiat-deposits";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/use-toast";
import axios from "axios";
import { Loader2 } from "lucide-react";
import { Transaction } from "@/types/transaction";

interface FiatTransaction {
  id: string;
  fiat_amount: number;
  currency: string;
  status: string;
  created_at: string;
  coin_currency: string;
  hash: string;
  updated_at: string;
  trade_id?: number;
}

// Function to convert FiatTransaction to Transaction type
const convertFiatTransactionToTransaction = (
  transaction: FiatTransaction,
  type: "deposit" | "withdrawal",
): Transaction => {
  return {
    ...transaction,
    amount: transaction.fiat_amount,
    type: type === "deposit" ? "fiat_deposit" : "fiat_withdrawal",
    coin_currency: transaction.currency,
    hash: "",
    updated_at: transaction.created_at,
    id: `${type}_${transaction.id}`,
    trade_id: transaction.trade_id,
  } as Transaction;
};

export default function FiatTransactionHistoryPage() {
  const t = useTranslations();
  const params = useParams();
  const router = useRouter();
  const { toast } = useToast();
  const currency = params.currency as string;

  const [depositHistory, setDepositHistory] = useState<FiatTransaction[]>([]);
  const [withdrawalHistory, setWithdrawalHistory] = useState<FiatTransaction[]>(
    [],
  );
  const [isLoadingDeposits, setIsLoadingDeposits] = useState(false);
  const [isLoadingWithdrawals, setIsLoadingWithdrawals] = useState(false);

  useEffect(() => {
    fetchDepositHistory();
    fetchWithdrawalHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currency]);

  const fetchDepositHistory = async () => {
    setIsLoadingDeposits(true);
    try {
      const response = await getFiatDeposits();
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
      setIsLoadingDeposits(false);
    }
  };

  const fetchWithdrawalHistory = async () => {
    setIsLoadingWithdrawals(true);
    try {
      const response = await getFiatWithdrawals();
      const filteredWithdrawals = Array.isArray(response)
        ? response
            .filter(
              (withdrawal) =>
                withdrawal.currency.toLowerCase() === currency.toLowerCase(),
            )
            .map((w) => ({
              ...w,
              coin_currency: w.currency,
              hash: "",
              updated_at: w.created_at,
            }))
        : [];
      setWithdrawalHistory(filteredWithdrawals);
    } catch (error) {
      console.error("Error fetching withdrawal history:", error);
      toast({
        title: t("withdrawal.errorFetchingHistory", {
          fallback: "Failed to load withdrawal history",
        }),
        description: axios.isAxiosError(error)
          ? error.response?.data?.message
          : t("withdrawal.somethingWentWrong", {
              fallback: "Something went wrong",
            }),
        variant: "destructive",
      });
    } finally {
      setIsLoadingWithdrawals(false);
    }
  };

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

  const handleViewDetails = (transaction: Transaction) => {
    // Type guard to check if transaction has trade_id property
    if (!("trade_id" in transaction)) {
      console.error("Transaction does not have trade_id property", transaction);
      return;
    }

    const detailsUrl = `/${params.locale}/trade/${transaction.trade_id}`;
    router.push(detailsUrl);
  };

  // Convert fiat transactions to transaction format
  const fiatDeposits = depositHistory.map((tx) =>
    convertFiatTransactionToTransaction(tx, "deposit"),
  );
  const fiatWithdrawals = withdrawalHistory.map((tx) =>
    convertFiatTransactionToTransaction(tx, "withdrawal"),
  );

  // Combine all transactions and sort by date
  const allTransactions = [...fiatDeposits, ...fiatWithdrawals].sort(
    (a, b) =>
      new Date(b.created_at).getTime() - new Date(a.created_at).getTime(),
  );

  const isLoading = isLoadingDeposits || isLoadingWithdrawals;

  return (
    <ProtectedLayout>
      <div className="container py-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>
              {t("wallet.history.title")} - {currency.toUpperCase()}
            </CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                fetchDepositHistory();
                fetchWithdrawalHistory();
              }}
              disabled={isLoading}
            >
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : null}
              {t("deposit.refreshHistory", { fallback: "Refresh History" })}
            </Button>
          </CardHeader>
          <CardContent>
            <TransactionHistoryTabs
              transactions={allTransactions}
              isLoading={isLoading}
              statusClassFn={getStatusClass}
              onViewDetails={handleViewDetails}
              showViewDetailsButton
            />
          </CardContent>
        </Card>
      </div>
    </ProtectedLayout>
  );
}
