"use client";

import {
  Transaction,
  isFiatTransaction,
  TRANSACTION_STATUS,
  isCryptoTransaction,
} from "@/types/transaction";
import { formatDate } from "@/lib/utils";
import { useTranslations } from "next-intl";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { SortableHeader } from "@/components/sortable-header";
import { useMemo, useState } from "react";
import { SortKey, SortState } from "@/types/sort";
import { ArrowDown, ArrowUp, ExternalLink, Copy } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

interface TransactionHistoryProps {
  transactions: Transaction[];
  isLoading?: boolean;
  statusClassFn?: (status: string) => string;
  onViewDetails?: (transaction: Transaction) => void;
  showViewDetailsButton?: boolean;
}

const STATUS_CLASSES = {
  [TRANSACTION_STATUS.COMPLETED]: "bg-green-100 text-green-700",
  [TRANSACTION_STATUS.PENDING]: "bg-yellow-100 text-yellow-700",
  [TRANSACTION_STATUS.FAILED]: "bg-red-100 text-red-700",
  [TRANSACTION_STATUS.CANCELLED]: "bg-gray-100 text-gray-700",
  [TRANSACTION_STATUS.PROCESSING]: "bg-yellow-100 text-yellow-700",
  [TRANSACTION_STATUS.VERIFIED]: "bg-green-100 text-green-700",
} as const;

const SKELETON_ROWS = 5;

// Helper function to truncate middle of string
const truncateMiddle = (str: string, start = 6, end = 6) => {
  if (str.length <= start + end) return str;
  return `${str.slice(0, start)}...${str.slice(-end)}`;
};

// Helper function to get explorer URL
const getExplorerUrl = (transaction: Transaction): string | null => {
  if (!isCryptoTransaction(transaction) || !transaction.hash) return null;

  // Determine network based on coin_currency
  const currency = transaction.coin_currency?.toLowerCase();
  const hash = transaction.hash;

  // For different networks - you may need to adjust these based on your actual network detection logic
  if (currency === "btc") {
    return `https://blockstream.info/tx/${hash}`;
  } else if (currency === "eth" || currency === "usdt" || currency === "usdc") {
    return `https://etherscan.io/tx/${hash}`;
  } else if (currency === "bnb") {
    return `https://bscscan.com/tx/${hash}`;
  } else if (currency === "trx") {
    return `https://tronscan.org/#/transaction/${hash}`;
  } else if (currency === "sol") {
    return `https://solscan.io/tx/${hash}`;
  }

  // Default to etherscan for unknown currencies
  return `https://etherscan.io/tx/${hash}`;
};

export function TransactionHistory({
  transactions,
  isLoading = false,
  statusClassFn,
  onViewDetails,
  showViewDetailsButton = false,
}: TransactionHistoryProps) {
  const t = useTranslations();
  const [sort, setSort] = useState<SortState>({
    key: "date",
    direction: "desc",
  });

  const handleSort = (key: SortKey) => {
    setSort((prev) => ({
      key,
      direction: prev.key === key && prev.direction === "asc" ? "desc" : "asc",
    }));
  };

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast.success(`${label} copied to clipboard`);
  };

  const sortedTransactions = useMemo(() => {
    const direction = sort.direction === "asc" ? 1 : -1;
    return [...transactions].sort((a, b) => {
      switch (sort.key) {
        case "type":
          return direction * a.type.localeCompare(b.type);
        case "amount":
          return direction * (a.amount - b.amount);
        case "status":
          return direction * a.status.localeCompare(b.status);
        case "date":
          return (
            direction *
            (new Date(a.created_at).getTime() -
              new Date(b.created_at).getTime())
          );
        default:
          return 0;
      }
    });
  }, [transactions, sort]);

  const getStatusClass = (transaction: Transaction) => {
    if (isFiatTransaction(transaction) && statusClassFn) {
      console.log(
        `statusClassFn: ${statusClassFn(transaction.status)}, status: ${transaction.status}`,
      );
      return statusClassFn(transaction.status);
    }
    return (
      STATUS_CLASSES[transaction.status] ||
      STATUS_CLASSES[TRANSACTION_STATUS.FAILED]
    );
  };

  const getTransactionIcon = (transaction: Transaction) => {
    const isDeposit = transaction.type.includes("deposit");
    return isDeposit ? (
      <ArrowDown className="h-4 w-4 text-green-500" data-testid="arrow-down" />
    ) : (
      <ArrowUp className="h-4 w-4 text-red-500" data-testid="arrow-up" />
    );
  };

  const getTransactionCurrency = (transaction: Transaction) => {
    if (isFiatTransaction(transaction)) {
      return transaction.currency.toUpperCase();
    }
    if ("coin_currency" in transaction) {
      return transaction.coin_currency?.toUpperCase() || "";
    }
    return "";
  };

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: SKELETON_ROWS }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full" data-testid="skeleton" />
        ))}
      </div>
    );
  }

  if (!transactions.length) {
    return (
      <div className="text-center py-4 text-muted-foreground">
        {t("wallet.history.noTransactions")}
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <SortableHeader
            label={t("wallet.history.type")}
            sortKey="type"
            currentSort={sort}
            onSort={handleSort}
          />
          <SortableHeader
            label={t("wallet.history.amount")}
            sortKey="amount"
            currentSort={sort}
            onSort={handleSort}
          />
          <SortableHeader
            label={t("wallet.history.status")}
            sortKey="status"
            currentSort={sort}
            onSort={handleSort}
          />
          <SortableHeader
            label={t("wallet.history.date")}
            sortKey="date"
            currentSort={sort}
            onSort={handleSort}
          />
          <TableHead>
            {showViewDetailsButton
              ? t("wallet.history.actions")
              : t("wallet.history.hash")}
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sortedTransactions.map((transaction) => (
          <TableRow key={transaction.id}>
            <TableCell>
              <div className="flex items-center gap-2">
                {getTransactionIcon(transaction)}
                <span className="capitalize">
                  {t(`wallet.history.${transaction.type}`, {
                    fallback: transaction.type.replace("_", " "),
                  })}
                </span>
              </div>
            </TableCell>
            <TableCell>
              {transaction.amount} {getTransactionCurrency(transaction)}
            </TableCell>
            <TableCell>
              <span
                className={cn(
                  "inline-flex items-center rounded-full px-2 py-1 text-xs font-medium",
                  getStatusClass(transaction),
                )}
              >
                {transaction.status}
              </span>
            </TableCell>
            <TableCell>{formatDate(transaction.created_at)}</TableCell>
            <TableCell>
              {showViewDetailsButton ? (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onViewDetails?.(transaction)}
                  className="flex items-center gap-1"
                >
                  {t("wallet.history.viewDetails")}
                  <ExternalLink className="h-4 w-4" />
                </Button>
              ) : (
                <div className="flex items-center gap-1">
                  {isFiatTransaction(transaction) ? (
                    <span className="font-mono text-sm text-muted-foreground">
                      {t("wallet.history.fiatTransaction", {
                        fallback: "Fiat Transaction",
                      })}
                    </span>
                  ) : isCryptoTransaction(transaction) && transaction.hash ? (
                    <>
                      <span
                        className="font-mono text-sm"
                        title={transaction.hash}
                      >
                        {truncateMiddle(transaction.hash)}
                      </span>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-6 w-6"
                        onClick={() =>
                          handleCopy(transaction.hash, "Transaction hash")
                        }
                        title="Copy transaction hash"
                      >
                        <Copy className="h-3 w-3" />
                      </Button>
                      {getExplorerUrl(transaction) && (
                        <a
                          href={getExplorerUrl(transaction)!}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6"
                            title="View on explorer"
                          >
                            <ExternalLink className="h-3 w-3" />
                          </Button>
                        </a>
                      )}
                    </>
                  ) : (
                    <span className="font-mono text-sm text-muted-foreground">
                      -
                    </span>
                  )}
                </div>
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
