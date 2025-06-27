"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { useTranslations } from "next-intl";
import { ArrowDown, ArrowUp } from "lucide-react";

// Mock data
const mockTransactions = [
  {
    id: 1,
    type: "deposit",
    amount: "0.5",
    currency: "BTC",
    status: "completed",
    date: "2024-03-20T10:30:00Z",
    hash: "0x1234...5678",
  },
  {
    id: 2,
    type: "withdraw",
    amount: "1.2",
    currency: "ETH",
    status: "pending",
    date: "2024-03-19T15:45:00Z",
    hash: "0x8765...4321",
  },
  {
    id: 3,
    type: "deposit",
    amount: "100",
    currency: "USDT",
    status: "completed",
    date: "2024-03-18T08:15:00Z",
    hash: "0xabcd...efgh",
  },
  {
    id: 4,
    type: "withdraw",
    amount: "50",
    currency: "USDT",
    status: "failed",
    date: "2024-03-17T20:20:00Z",
    hash: "0xijkl...mnop",
  },
];

export default function TransactionHistoryPage() {
  const t = useTranslations();

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
    <div className="container py-8">
      <Card>
        <CardHeader>
          <CardTitle>{t("wallet.history.title")}</CardTitle>
          <CardDescription>{t("wallet.history.description")}</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("wallet.history.type")}</TableHead>
                <TableHead>{t("wallet.history.amount")}</TableHead>
                <TableHead>{t("wallet.history.status")}</TableHead>
                <TableHead>{t("wallet.history.date")}</TableHead>
                <TableHead>{t("wallet.history.hash")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {mockTransactions.map((transaction) => (
                <TableRow key={transaction.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      {transaction.type === "deposit" ? (
                        <ArrowDown
                          data-testid="arrow-down"
                          className="h-4 w-4 text-green-500"
                        />
                      ) : (
                        <ArrowUp
                          data-testid="arrow-up"
                          className="h-4 w-4 text-red-500"
                        />
                      )}
                      <span className="capitalize">{transaction.type}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="font-medium">
                      {transaction.amount} {transaction.currency}
                    </div>
                  </TableCell>
                  <TableCell>
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                        transaction.status === "completed"
                          ? "bg-green-100 text-green-700"
                          : transaction.status === "pending" ||
                              transaction.status === "processing"
                            ? "bg-yellow-100 text-yellow-700"
                            : "bg-yellow-100 text-red-700"
                      }`}
                    >
                      {transaction.status}
                    </span>
                  </TableCell>
                  <TableCell>
                    {new Date(transaction.date).toLocaleString()}
                  </TableCell>
                  <TableCell>
                    <span className="font-mono text-sm">
                      {transaction.hash}
                    </span>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
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
