"use client";

import { useEffect, useState, useCallback } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatDistanceToNow } from "date-fns";
import { vi } from "date-fns/locale";
import { formatCurrency } from "@/lib/utils/format";
import { useTranslations } from "next-intl";
import { SwapOrder, fetchSwapOrderDetail } from "@/lib/api/amm-orders";
import { Loader2 } from "lucide-react";
import { useAmmOrderChannel } from "@/hooks/use-amm-order-channel";
import { useAuth } from "@/hooks/use-auth";

interface SwapDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  swapId: number | null;
}

export function SwapDetailDialog({
  open,
  onOpenChange,
  swapId,
}: SwapDetailDialogProps) {
  const t = useTranslations("swap.history");
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [swapDetail, setSwapDetail] = useState<SwapOrder | null>(null);

  // Helper to fetch detail
  const fetchDetail = useCallback(() => {
    if (swapId && open) {
      setIsLoading(true);
      setError(null);
      fetchSwapOrderDetail(swapId)
        .then((data) => {
          setSwapDetail(data);
        })
        .catch((err) => {
          console.error("Error fetching swap detail:", err);
          setError(t("detailError"));
        })
        .finally(() => {
          setIsLoading(false);
        });
    } else {
      setSwapDetail(null);
    }
  }, [swapId, open, t]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  // Polling để cập nhật trạng thái chi tiết swap order
  useEffect(() => {
    if (!open || !swapId) return;
    let pollCount = 0;
    const MAX_POLL_ATTEMPTS = 30; // 100ms * 30 = 3s
    let stopped = false;
    const interval = setInterval(async () => {
      pollCount++;
      try {
        const detail = await fetchSwapOrderDetail(swapId);
        setSwapDetail(detail);
        if (
          pollCount >= MAX_POLL_ATTEMPTS ||
          (detail.status !== "pending" && detail.status !== "processing")
        ) {
          if (!stopped) {
            clearInterval(interval);
            stopped = true;
          }
        }
      } catch {}
    }, 100);
    return () => {
      clearInterval(interval);
      stopped = true;
    };
  }, [open, swapId]);

  // WebSocket realtime update
  useAmmOrderChannel({
    userId: user?.id || 0,
    onOrderUpdated: (orderId) => {
      if (open && swapId === orderId) {
        fetchDetail();
      }
    },
    onOrderCompleted: (orderId) => {
      if (open && swapId === orderId) {
        fetchDetail();
      }
    },
    onOrderFailed: (orderId) => {
      if (open && swapId === orderId) {
        fetchDetail();
      }
    },
  });

  // Format trạng thái giao dịch
  const formatStatus = (status: string) => {
    switch (status.toLowerCase()) {
      case "pending":
        return (
          <Badge className="bg-orange-500 hover:bg-orange-600">
            {t("status.pending")}
          </Badge>
        );
      case "processing":
        return (
          <Badge className="bg-blue-500 hover:bg-blue-600">
            {t("status.processing")}
          </Badge>
        );
      case "success":
        return (
          <Badge className="bg-green-500 hover:bg-green-600">
            {t("status.success")}
          </Badge>
        );
      case "error":
        return <Badge variant="destructive">{t("status.error")}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  // Format thời gian
  const formatTime = (timestamp: number) => {
    if (!timestamp) return "N/A";
    try {
      const date = new Date(timestamp * 1000);
      return formatDistanceToNow(date, { addSuffix: true, locale: vi });
    } catch (error) {
      console.error("Error formatting time:", error);
      return "N/A";
    }
  };

  // Format số tiền
  const formatAmount = (amount?: string) => {
    try {
      return formatCurrency(amount || "0", "", {
        decimals: 6,
        showSymbol: false,
      });
    } catch (error) {
      console.error("Error formatting amount:", error);
      return "0";
    }
  };

  const formatFees = (fees?: Record<string, number>) => {
    if (!fees || typeof fees !== "object" || Object.keys(fees).length === 0) {
      return <span>{t("noFees")}</span>;
    }

    return (
      <>
        {Object.entries(fees).map(([key, value]) => (
          <div key={key}>
            {formatCurrency(value.toString(), key.toUpperCase(), {
              decimals: 6,
            })}
          </div>
        ))}
      </>
    );
  };

  // Format token pair từ swap order
  const formatTokenPair = (identifier?: string, zeroForOne?: boolean) => {
    // Trong thực tế, có thể cần thêm dữ liệu về pool để biết cặp token
    // Giả định order.identifier có định dạng như "amm_order_2_usdt_vnd_1746618891"
    try {
      if (identifier) {
        const parts = identifier.split("_");
        // Tìm phần tương ứng với tên token
        for (let i = 0; i < parts.length - 1; i++) {
          if (parts[i] === "usdt" && parts[i + 1] === "vnd") {
            if (zeroForOne) {
              return `USDT → VND`;
            } else {
              return `VND → USDT`;
            }
          }
        }
      }
      // Nếu không tìm thấy theo format trên, thử phương pháp khác
      if (identifier && identifier.includes("usdt_vnd")) {
        if (zeroForOne) {
          return `USDT → VND`;
        } else {
          return `VND → USDT`;
        }
      }

      return "N/A";
    } catch (error) {
      console.error("Error formatting token pair:", error);
      return "N/A";
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t("detailTitle")}</DialogTitle>
          <DialogDescription>{t("detailDescription")}</DialogDescription>
        </DialogHeader>

        {isLoading && (
          <div className="flex items-center justify-center py-8">
            <Loader2
              data-testid="loading-icon"
              className="h-8 w-8 animate-spin"
            />
          </div>
        )}

        {error && (
          <div
            className="text-red-500 text-center py-4"
            data-testid="error-message"
          >
            {error}
          </div>
        )}

        {swapDetail && !isLoading && !error && (
          <Card>
            <CardHeader>
              <CardTitle data-testid="token-pair">
                {formatTokenPair(
                  swapDetail.identifier,
                  swapDetail.zero_for_one,
                )}
              </CardTitle>
              <CardDescription data-testid="order-id">
                {t("columns.id")}: {swapDetail.id}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">
                    {t("columns.status")}
                  </div>
                  <div data-testid="status-badge">
                    {formatStatus(swapDetail.status || "unknown")}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    {t("columns.time")}
                  </div>
                  <div data-testid="created-time">
                    {formatTime(swapDetail.created_at)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    {t("columns.amountSent")}
                  </div>
                  <div data-testid="amount-sent">
                    {formatAmount(swapDetail.amount_specified)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    {t("columns.amountReceived")}
                  </div>
                  <div data-testid="amount-received">
                    {formatAmount(swapDetail.amount_received)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    {t("columns.fees")}
                  </div>
                  <div data-testid="fees">{formatFees(swapDetail.fees)}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    {t("slippage")}
                  </div>
                  <div data-testid="slippage">
                    {parseFloat(swapDetail.slippage) * 100}%
                  </div>
                </div>
              </div>

              {swapDetail.error_message && (
                <div
                  className="mt-4 p-3 bg-red-50 text-red-800 rounded-md"
                  data-testid="error-message"
                >
                  {swapDetail.error_message}
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </DialogContent>
    </Dialog>
  );
}
