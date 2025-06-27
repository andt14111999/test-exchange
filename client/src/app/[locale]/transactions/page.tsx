"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useTrades } from "@/lib/api/hooks/use-trades";
import { useUserStore } from "@/lib/store/user-store";
import { formatFiatAmount } from "@/lib/utils/index";
import { Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import { ApiTrade } from "@/lib/api/trades";
import { ProtectedLayout } from "@/components/protected-layout";

// Define both possible response formats
interface PaginatedApiResponse {
  data: ApiTrade[];
  meta: {
    total_pages: number;
    total_count: number;
    current_page: number;
  };
}

// Components
const Pagination = ({
  currentPage,
  totalPages,
  onPageChange,
  t,
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  t: ReturnType<typeof useTranslations>;
}) => (
  <div className="flex items-center justify-center gap-2">
    <Button
      variant="outline"
      size="sm"
      onClick={() => onPageChange(currentPage - 1)}
      disabled={currentPage <= 1}
    >
      {t("previous")}
    </Button>
    <span className="text-sm">
      {t("page", { current: currentPage, total: totalPages })}
    </span>
    <Button
      variant="outline"
      size="sm"
      onClick={() => onPageChange(currentPage + 1)}
      disabled={currentPage >= totalPages}
    >
      {t("next")}
    </Button>
  </div>
);

const TradeStatusBadge = ({
  status,
  t,
}: {
  status: string;
  t: ReturnType<typeof useTranslations>;
}) => {
  const getStatusStyle = (status: string) => {
    switch (status) {
      case "released":
        return "bg-emerald-100 text-emerald-800";
      case "cancelled":
        return "bg-gray-100 text-gray-800";
      case "disputed":
        return "bg-amber-100 text-amber-800";
      case "paid":
        return "bg-blue-100 text-blue-800";
      case "unpaid":
        return "bg-yellow-100 text-yellow-800";
      default:
        return "bg-yellow-100 text-yellow-800";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "released":
        return t("statusCompleted", { fallback: "COMPLETED" });
      case "cancelled":
        return t("statusCancelled", { fallback: "CANCELLED" });
      case "awaiting":
      case "unpaid":
        return t("statusPending", { fallback: "PENDING" });
      case "paid":
        return t("statusPaid", { fallback: "PAID" });
      case "disputed":
        return t("statusDisputed", { fallback: "DISPUTED" });
      default:
        return status.toUpperCase();
    }
  };

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${getStatusStyle(
        status,
      )}`}
    >
      {getStatusText(status)}
    </span>
  );
};

const TradeTypeTag = ({
  trade,
  user,
  t,
}: {
  trade: ApiTrade;
  user: { role: string; id: string } | null;
  t: ReturnType<typeof useTranslations>;
}) => {
  const getTradeType = () => {
    const isMerchant = user?.role === "merchant";
    if (isMerchant && trade?.seller?.id) {
      return Number(trade.seller.id) === Number(user?.id) ? "BUY" : "SELL";
    }
    return trade?.taker_side === "buy" ? "BUY" : "SELL";
  };

  const type = getTradeType();
  const style =
    type === "SELL" ? "bg-red-100 text-red-800" : "bg-green-100 text-green-800";

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${style}`}
    >
      {type === "SELL" ? t("sell") : t("buy")}
    </span>
  );
};

export default function TransactionHistory() {
  const router = useRouter();
  const t = useTranslations("merchant.transactions");
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);
  const { data, isLoading, error } = useTrades({ page, per_page: perPage }) as {
    data: ApiTrade[] | PaginatedApiResponse | undefined;
    isLoading: boolean;
    error: Error | null;
  };
  const { user } = useUserStore();

  // Debug data structure
  useEffect(() => {
    if (data) {
      console.log("Trade data structure:", data);
    }
  }, [data]);

  if (isLoading) {
    return (
      <ProtectedLayout>
        <div className="flex justify-center items-center py-8">
          <Loader2
            data-testid="loading-spinner"
            className="h-8 w-8 animate-spin text-primary"
          />
        </div>
      </ProtectedLayout>
    );
  }

  if (error) {
    return (
      <ProtectedLayout>
        <div className="text-center py-8 text-red-500">{t("error")}</div>
      </ProtectedLayout>
    );
  }

  // Check if data exists - handle both API response formats
  const trades = Array.isArray(data) ? data : data?.data || [];
  const hasNoTrades = trades.length === 0;

  if (hasNoTrades) {
    return (
      <ProtectedLayout>
        <div className="text-center py-8 text-muted-foreground">
          {t("noTransactionsFound")}
        </div>
      </ProtectedLayout>
    );
  }

  // Handle pagination metadata in both formats
  const paginatedData = !Array.isArray(data)
    ? (data as PaginatedApiResponse)
    : null;
  const totalPages = paginatedData?.meta?.total_pages || 1;

  const handlePageChange = (newPage: number) => setPage(newPage);

  const handlePerPageChange = (value: string) => {
    setPerPage(parseInt(value));
    setPage(1);
  };

  return (
    <ProtectedLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold tracking-tight">{t("title")}</h1>

          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">
              {t("itemsPerPage")}:
            </span>
            <Select
              value={perPage.toString()}
              onValueChange={handlePerPageChange}
            >
              <SelectTrigger className="w-[80px]">
                <SelectValue placeholder={perPage.toString()} />
              </SelectTrigger>
              <SelectContent>
                {[10, 20, 50, 100].map((value) => (
                  <SelectItem key={value} value={value.toString()}>
                    {value}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>
              {t("title", { fallback: "Transaction History" })}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="relative w-full overflow-auto">
              <table className="w-full caption-bottom text-sm">
                <thead>
                  <tr className="border-b">
                    {[
                      "date",
                      "reference",
                      "type",
                      "amount",
                      "currency",
                      "status",
                      "actions",
                    ].map((header) => (
                      <th
                        key={header}
                        className="h-12 px-4 text-left align-middle font-medium"
                      >
                        {t(header)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {trades.map((trade: ApiTrade) => (
                    <tr key={trade.id} className="border-b">
                      <td className="p-4 align-middle">
                        {new Date(trade.created_at).toLocaleDateString()}
                      </td>
                      <td className="p-4 align-middle">{trade.ref}</td>
                      <td className="p-4 align-middle">
                        <TradeTypeTag trade={trade} user={user} t={t} />
                      </td>
                      <td className="p-4 align-middle">
                        {formatFiatAmount(
                          parseFloat(trade.fiat_amount),
                          trade.fiat_currency,
                        )}
                      </td>
                      <td className="p-4 align-middle">
                        {trade.fiat_currency}
                      </td>
                      <td className="p-4 align-middle">
                        <TradeStatusBadge status={trade.status} t={t} />
                      </td>
                      <td className="p-4 align-middle">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => router.push(`/trade/${trade.id}`)}
                        >
                          {t("view")}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="mt-4">
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={handlePageChange}
                  t={t}
                />
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </ProtectedLayout>
  );
}
