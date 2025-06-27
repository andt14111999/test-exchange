import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { TransactionHistory } from "@/components/transaction-history";
import { useTranslations } from "next-intl";
import { Transaction, TRANSACTION_TYPE } from "@/types/transaction";
import { useMemo } from "react";

export interface TransactionHistoryTabsProps {
  transactions: Transaction[];
  isLoading: boolean;
  statusClassFn?: (status: string) => string;
  onViewDetails?: (transaction: Transaction) => void;
  showViewDetailsButton?: boolean;
}

export function TransactionHistoryTabs({
  transactions,
  isLoading,
  statusClassFn,
  onViewDetails,
  showViewDetailsButton,
}: TransactionHistoryTabsProps) {
  const t = useTranslations();

  const { deposits, withdrawals } = useMemo(() => {
    const deposits: Transaction[] = [];
    const withdrawals: Transaction[] = [];

    transactions.forEach((tx) => {
      if (tx.type.includes(TRANSACTION_TYPE.DEPOSIT)) {
        deposits.push(tx);
      } else if (tx.type.includes(TRANSACTION_TYPE.WITHDRAWAL)) {
        withdrawals.push(tx);
      }
    });

    return { deposits, withdrawals };
  }, [transactions]);

  return (
    <Tabs defaultValue="all" className="w-full">
      <TabsList className="grid w-full grid-cols-3">
        <TabsTrigger value="all">{t("wallet.history.all")}</TabsTrigger>
        <TabsTrigger value="deposit">{t("wallet.history.deposit")}</TabsTrigger>
        <TabsTrigger value="withdraw">
          {t("wallet.history.withdraw")}
        </TabsTrigger>
      </TabsList>
      <TabsContent value="all">
        <TransactionHistory
          transactions={transactions}
          isLoading={isLoading}
          statusClassFn={statusClassFn}
          onViewDetails={onViewDetails}
          showViewDetailsButton={showViewDetailsButton}
        />
      </TabsContent>
      <TabsContent value="deposit">
        <TransactionHistory
          transactions={deposits}
          isLoading={isLoading}
          statusClassFn={statusClassFn}
          onViewDetails={onViewDetails}
          showViewDetailsButton={showViewDetailsButton}
        />
      </TabsContent>
      <TabsContent value="withdraw">
        <TransactionHistory
          transactions={withdrawals}
          isLoading={isLoading}
          statusClassFn={statusClassFn}
          onViewDetails={onViewDetails}
          showViewDetailsButton={showViewDetailsButton}
        />
      </TabsContent>
    </Tabs>
  );
}
