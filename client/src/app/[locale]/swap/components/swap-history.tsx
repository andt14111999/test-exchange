"use client";

import { useCallback, useState, useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { SwapOrder, fetchSwapOrders } from "@/lib/api/amm-orders";
import { formatDistanceToNow } from "date-fns";
import { vi } from "date-fns/locale";
import { Badge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/utils/format";
import { ArrowLeftIcon, ArrowRightIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { SwapDetailDialog } from "./swap-detail-dialog";
import { useAmmOrderChannel } from "@/hooks/use-amm-order-channel";
import { useAuth } from "@/hooks/use-auth";
import { toast } from "sonner";

export function SwapHistory() {
  const t = useTranslations("swap.history");
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [currentPage, setCurrentPage] = useState(1);
  const [activeStatus, setActiveStatus] = useState<string>("all");
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);

  // Sử dụng React Query để fetch danh sách swap orders
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["swap-orders", currentPage, activeStatus],
    queryFn: () => fetchSwapOrders(currentPage, 10, activeStatus),
    refetchOnMount: true,
    staleTime: 0,
  });

  // Polling để luôn cập nhật trạng thái mới nhất
  useEffect(() => {
    let pollCount = 0;
    const MAX_POLL_ATTEMPTS = 30; // 100ms * 30 = 3s
    const interval = setInterval(() => {
      pollCount++;
      refetch();
      // Nếu đã polling đủ số lần hoặc không còn order nào ở trạng thái cần theo dõi thì dừng
      if (
        pollCount >= MAX_POLL_ATTEMPTS ||
        (data &&
          data.amm_orders &&
          data.amm_orders.every(
            (order) =>
              order.status !== "pending" && order.status !== "processing",
          ))
      ) {
        clearInterval(interval);
      }
    }, 100); // 100ms
    return () => clearInterval(interval);
  }, [refetch, data]);

  // WebSocket handlers for real-time updates
  const handleOrderCreated = useCallback(() => {
    // Invalidate all swap order queries
    queryClient.invalidateQueries({ queryKey: ["swap-orders"] });

    // Show toast notification
    toast.success(t("orderCreated"), {
      description: t("orderCreatedSuccess"),
    });
  }, [queryClient, t]);

  const handleOrderUpdated = useCallback(() => {
    // Invalidate all swap order queries
    queryClient.invalidateQueries({ queryKey: ["swap-orders"] });

    // Show toast notification
    toast.info(t("orderUpdated"), {
      description: t("orderUpdatedSuccess"),
    });
  }, [queryClient, t]);

  const handleOrderCompleted = useCallback(() => {
    // Invalidate all swap order queries
    queryClient.invalidateQueries({ queryKey: ["swap-orders"] });

    // Show toast notification
    toast.success(t("orderCompleted"), {
      description: t("orderCompletedSuccess"),
    });
  }, [queryClient, t]);

  const handleOrderFailed = useCallback(() => {
    // Invalidate all swap order queries
    queryClient.invalidateQueries({ queryKey: ["swap-orders"] });

    // Show toast notification
    toast.error(t("orderFailed"), {
      description: t("orderFailedError"),
    });
  }, [queryClient, t]);

  const handleError = useCallback(
    (error: string) => {
      console.error("AmmOrderChannel error:", error);
      toast.error(t("websocketError"), {
        description: error,
      });
    },
    [t],
  );

  // Connect to AmmOrderChannel for real-time updates
  useAmmOrderChannel({
    userId: user?.id || 0,
    onOrderCreated: handleOrderCreated,
    onOrderUpdated: handleOrderUpdated,
    onOrderCompleted: handleOrderCompleted,
    onOrderFailed: handleOrderFailed,
    onError: handleError,
  });

  // Hàm helper để định dạng số tiền
  const formatAmount = useCallback((amount?: string) => {
    try {
      return formatCurrency(amount || "0", "", {
        decimals: 6,
        showSymbol: false,
      });
    } catch (error) {
      console.error("Error formatting amount:", error);
      return "0";
    }
  }, []);

  // Hàm helper để định dạng phí
  const formatFees = useCallback(
    (fees?: Record<string, number>) => {
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
    },
    [t],
  );

  // Format trạng thái giao dịch
  const formatStatus = useCallback(
    (status: string) => {
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
    },
    [t],
  );

  // Format thời gian tạo
  const formatTime = useCallback((timestamp: number) => {
    if (!timestamp) return "N/A";
    try {
      const date = new Date(timestamp * 1000);
      return formatDistanceToNow(date, { addSuffix: true, locale: vi });
    } catch (error) {
      console.error("Error formatting time:", error);
      return "N/A";
    }
  }, []);

  // Format token pair từ swap order
  const formatTokenPair = useCallback((order: SwapOrder) => {
    // Trong thực tế, có thể cần thêm dữ liệu về pool để biết cặp token
    // Giả định order.identifier có định dạng như "amm_order_2_usdt_vnd_1746618891"
    try {
      if (order.identifier) {
        const parts = order.identifier.split("_");
        // Tìm phần tương ứng với tên token
        for (let i = 0; i < parts.length - 1; i++) {
          if (parts[i] === "usdt" && parts[i + 1] === "vnd") {
            if (order.zero_for_one) {
              return `USDT → VND`;
            } else {
              return `VND → USDT`;
            }
          }
        }
      }
      // Nếu không tìm thấy theo format trên, thử phương pháp khác
      if (order.identifier && order.identifier.includes("usdt_vnd")) {
        if (order.zero_for_one) {
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
  }, []);

  // Xử lý chuyển trang
  const handlePageChange = useCallback((newPage: number) => {
    setCurrentPage(newPage);
  }, []);

  // Xử lý thay đổi bộ lọc trạng thái
  const handleStatusChange = useCallback((status: string) => {
    setActiveStatus(status);
    setCurrentPage(1); // Reset về trang 1 khi thay đổi filter
  }, []);

  // Xử lý click vào một record
  const handleRowClick = useCallback((orderId: number) => {
    setSelectedOrderId(orderId);
    setDetailDialogOpen(true);
  }, []);

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t("title")}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
          <p className="text-center text-muted-foreground">{t("loading")}</p>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t("title")}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="text-center py-4">
            <p className="text-red-500 mb-4">{t("error")}</p>
            <Button onClick={() => refetch()}>{t("retry")}</Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>{t("title")}</CardTitle>
        </CardHeader>
        <CardContent>
          <Tabs
            defaultValue="all"
            value={activeStatus}
            onValueChange={handleStatusChange}
            className="mb-6"
          >
            <TabsList className="grid grid-cols-5 mb-4">
              <TabsTrigger value="all">{t("statusFilters.all")}</TabsTrigger>
              <TabsTrigger value="pending">
                {t("statusFilters.pending")}
              </TabsTrigger>
              <TabsTrigger value="processing">
                {t("statusFilters.processing")}
              </TabsTrigger>
              <TabsTrigger value="success">
                {t("statusFilters.success")}
              </TabsTrigger>
              <TabsTrigger value="error">
                {t("statusFilters.error")}
              </TabsTrigger>
            </TabsList>
          </Tabs>

          {data && data.amm_orders && data.amm_orders.length > 0 ? (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("columns.id")}</TableHead>
                      <TableHead>{t("columns.pair")}</TableHead>
                      <TableHead>{t("columns.status")}</TableHead>
                      <TableHead>{t("columns.amountSent")}</TableHead>
                      <TableHead>{t("columns.amountReceived")}</TableHead>
                      <TableHead>{t("columns.fees")}</TableHead>
                      <TableHead>{t("columns.time")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.amm_orders.map((order) => (
                      <TableRow
                        key={order.id}
                        className="cursor-pointer hover:bg-muted"
                        onClick={() => handleRowClick(order.id)}
                      >
                        <TableCell>{order.id}</TableCell>
                        <TableCell>{formatTokenPair(order)}</TableCell>
                        <TableCell>
                          {formatStatus(order.status || "unknown")}
                        </TableCell>
                        <TableCell>
                          {formatAmount(order.amount_specified)}
                        </TableCell>
                        <TableCell>
                          {formatAmount(order.amount_received)}
                        </TableCell>
                        <TableCell>{formatFees(order.fees)}</TableCell>
                        <TableCell>{formatTime(order.created_at)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Phân trang */}
              {data.meta && (
                <div className="flex items-center justify-end space-x-2 py-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage <= 1}
                  >
                    <ArrowLeftIcon className="h-4 w-4 mr-2" />
                    {t("pagination.previous")}
                  </Button>
                  <div className="text-sm">
                    {t("pagination.page", {
                      current: currentPage,
                      total: data.meta.total_pages || 1,
                    })}
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={
                      !data.meta.next_page ||
                      currentPage >= data.meta.total_pages
                    }
                  >
                    {t("pagination.next")}
                    <ArrowRightIcon className="h-4 w-4 ml-2" />
                  </Button>
                </div>
              )}
            </>
          ) : (
            <div className="text-center py-8">
              <p className="text-muted-foreground">{t("noOrders")}</p>
            </div>
          )}
        </CardContent>
      </Card>

      <SwapDetailDialog
        open={detailDialogOpen}
        onOpenChange={setDetailDialogOpen}
        swapId={selectedOrderId}
      />
    </>
  );
}
