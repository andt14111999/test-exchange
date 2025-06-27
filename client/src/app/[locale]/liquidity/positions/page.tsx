"use client";

import { useState, useCallback, useEffect } from "react";
import { useTranslations } from "next-intl";
import {
  AmmPosition,
  collectFee,
  closePosition,
  fetchPositions,
} from "@/lib/api/positions";
import PositionItem from "./components/PositionItem";
import PositionDetail from "./components/PositionDetail";
import StatusTabs, { TabStatus } from "./components/StatusTabs";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast, Toaster } from "sonner";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import { useAmmPositionChannel } from "@/hooks/use-positions-channel";
import { useUserStore } from "@/lib/store/user-store";

export default function PositionsPage() {
  const t = useTranslations("liquidity");
  const queryClient = useQueryClient();
  const { user } = useUserStore();
  const [selectedPosition, setSelectedPosition] = useState<AmmPosition | null>(
    null,
  );
  const [detailOpen, setDetailOpen] = useState(false);
  const [currentTab, setCurrentTab] = useState<TabStatus>("all");

  // Force refresh positions data when component mounts
  useEffect(() => {
    // Invalidate positions query to ensure fresh data is loaded
    queryClient.invalidateQueries({ queryKey: ["positions"] });

    // Also refresh after a small delay to handle any WebSocket timing issues
    const timeoutId = setTimeout(() => {
      queryClient.invalidateQueries({ queryKey: ["positions"] });
    }, 1000);

    // Additional refresh after 3 seconds to catch any delayed updates
    const timeoutId2 = setTimeout(() => {
      queryClient.invalidateQueries({ queryKey: ["positions"] });
    }, 3000);

    return () => {
      clearTimeout(timeoutId);
      clearTimeout(timeoutId2);
    };
  }, [queryClient]);

  // Fetch positions data
  const { data, isLoading } = useQuery({
    queryKey: ["positions", currentTab],
    queryFn: () => fetchPositions(currentTab),
  });

  // Force refresh data when tab changes
  useEffect(() => {
    queryClient.invalidateQueries({ queryKey: ["positions", currentTab] });
  }, [currentTab, queryClient]);

  // WebSocket handlers
  const handlePositionUpdate = useCallback(
    (position: AmmPosition) => {
      queryClient.invalidateQueries({ queryKey: ["positions"] });

      // Show toast notification for position updates
      toast.success(t("positionUpdated"), {
        description: `${position.pool_pair} ${t("positionUpdatedSuccess")}`,
      });
    },
    [queryClient, t],
  );

  const handlePositionCreated = useCallback(
    (position: AmmPosition) => {
      queryClient.invalidateQueries({ queryKey: ["positions"] });

      // Show toast notification for new positions
      toast.success(t("positionCreated"), {
        description: `${position.pool_pair} ${t("positionCreatedSuccess")}`,
      });
    },
    [queryClient, t],
  );

  const handlePositionClosed = useCallback(
    (positionId: number) => {
      // Invalidate all position-related queries
      queryClient.invalidateQueries({ queryKey: ["positions"] });
      queryClient.invalidateQueries({ queryKey: ["positions", "all"] });
      queryClient.invalidateQueries({ queryKey: ["positions", "open"] });
      queryClient.invalidateQueries({ queryKey: ["positions", "closed"] });

      // Show toast notification for closed positions
      toast.success(t("positionClosed"), {
        description: t("positionClosedSuccess"),
      });

      // Close detail dialog if the closed position was selected
      if (selectedPosition?.id === positionId) {
        setDetailOpen(false);
        setSelectedPosition(null);
      }
    },
    [queryClient, t, selectedPosition],
  );

  // Initialize WebSocket connection
  useAmmPositionChannel({
    userId: user?.id ? parseInt(user.id) : 0,
    onPositionUpdate: handlePositionUpdate,
    onPositionCreated: handlePositionCreated,
    onPositionClosed: handlePositionClosed,
  });

  // Claim fee mutation
  const claimFeeMutation = useMutation({
    mutationFn: (id: number) => collectFee(id),
    onSuccess: () => {
      toast.success(t("feeCollected"), {
        description: t("feeCollectedSuccess"),
      });
      queryClient.invalidateQueries({ queryKey: ["positions"] });
    },
    onError: (error) => {
      toast.error(t("error"), {
        description: t("feeCollectedError"),
      });
      console.error("Error collecting fee:", error);
    },
  });

  // Close position mutation
  const closePositionMutation = useMutation({
    mutationFn: (position: AmmPosition) => closePosition(position.id),
    onSuccess: (data, position) => {
      if (position) {
        // Start polling to check position status changes
        const pollInterval = setInterval(() => {
          queryClient.invalidateQueries({ queryKey: ["positions"] });

          // Check if position status has changed to closed
          const currentData = queryClient.getQueryData([
            "positions",
            currentTab,
          ]) as { amm_positions?: AmmPosition[] };
          if (currentData?.amm_positions) {
            const updatedPosition = currentData.amm_positions.find(
              (p: AmmPosition) => p.id === position.id,
            );
            if (updatedPosition && updatedPosition.status === "closed") {
              clearInterval(pollInterval);
              toast.success(t("positionClosed"), {
                description: t("positionClosedSuccess"),
              });
            }
          }
        }, 200); // Poll every 0.2 second

        // Stop polling after 3 seconds
        setTimeout(() => {
          clearInterval(pollInterval);
        }, 3000);
      } else {
        console.log(
          "Position closed successfully (no position in test):",
          data,
        );
      }

      // Invalidate all position-related queries
      queryClient.invalidateQueries({ queryKey: ["positions"] });
      queryClient.invalidateQueries({ queryKey: ["positions", "all"] });
      queryClient.invalidateQueries({ queryKey: ["positions", "open"] });
      queryClient.invalidateQueries({ queryKey: ["positions", "closed"] });

      toast.success(t("positionClosing"), {
        description: t("positionClosingSuccess"),
      });

      setDetailOpen(false);
      setSelectedPosition(null);
    },
    onError: (error) => {
      console.error("Error closing position:", error);
      toast.error(t("error"), {
        description: t("positionClosedError"),
      });
    },
  });

  const handleClaim = (id: number) => {
    claimFeeMutation.mutate(id);
  };

  const handleClose = (position: AmmPosition) => {
    closePositionMutation.mutate(position);
  };

  const handleViewDetail = (position: AmmPosition) => {
    setSelectedPosition(position);
    setDetailOpen(true);
  };

  return (
    <div className="space-y-4">
      <Toaster position="top-right" />

      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Link href="/liquidity/pools">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <h1 className="text-2xl font-bold">{t("yourPositions")}</h1>
        </div>
        <Link href="/liquidity/pools">
          <Button variant="outline">{t("selectPool")}</Button>
        </Link>
      </div>

      <StatusTabs currentTab={currentTab} onChange={setCurrentTab}>
        <div className="hidden lg:grid lg:grid-cols-6 gap-4 p-4 text-sm font-medium text-muted-foreground">
          <div className="col-span-1">{t("pair")}</div>
          <div className="col-span-1">{t("range")}</div>
          <div className="text-left">{t("token0")}</div>
          <div className="text-left">{t("token1")}</div>
          <div className="text-center">{t("status.title")}</div>
          <div className="text-center">{t("actions")}</div>
        </div>

        {isLoading ? (
          <div className="py-8 text-center text-muted-foreground">
            {t("loading")}
          </div>
        ) : data?.amm_positions && data.amm_positions.length > 0 ? (
          <div className="space-y-4">
            {data.amm_positions.map((position) => (
              <PositionItem
                key={position.id}
                position={position}
                onClaim={handleClaim}
                onClose={handleClose}
                onViewDetail={handleViewDetail}
              />
            ))}
          </div>
        ) : (
          <div className="py-12 text-center text-muted-foreground">
            {t("noPositions")}
          </div>
        )}
      </StatusTabs>

      <PositionDetail
        position={selectedPosition}
        isOpen={detailOpen}
        onClose={() => setDetailOpen(false)}
        onClaim={handleClaim}
        onClosePosition={handleClose}
      />
    </div>
  );
}
