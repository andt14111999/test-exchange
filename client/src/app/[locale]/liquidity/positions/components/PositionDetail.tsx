import { useTranslations } from "next-intl";
import { CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AmmPosition } from "@/lib/api/positions";
import { formatCurrency } from "@/lib/utils/format";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { TickMath } from "@/lib/amm/tick-math";
import { formatDisplayPrice } from "@/lib/amm/position-utils";
import { useState } from "react";
import ConfirmCloseDialog from "./ConfirmCloseDialog";

interface PositionDetailProps {
  position: AmmPosition | null;
  isOpen: boolean;
  onClose: () => void;
  onClaim: (id: number) => void;
  onClosePosition: (position: AmmPosition) => void;
}

export default function PositionDetail({
  position,
  isOpen,
  onClose,
  onClaim,
  onClosePosition,
}: PositionDetailProps) {
  const t = useTranslations("liquidity");
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);

  if (!position) return null;

  const feesValue =
    Number(position.tokens_owed0) + Number(position.tokens_owed1);
  const isOpenStatus = position.status === "open";
  // Check if total_estimate_fee_in_token0 is greater than 1 for collect fee button
  const canCollectFee = Number(position.total_estimate_fee_in_token0) > 1;

  // Tách cặp để lấy tên token
  const [token0, token1] = position.pool_pair.split("/");

  // Tính giá từ tick index
  const lowerPrice = TickMath.tickToPrice(position.tick_lower_index);
  const upperPrice = TickMath.tickToPrice(position.tick_upper_index);
  const currentPrice = 152.87; // Giả định giá hiện tại, trong thực tế nên lấy từ API

  const handleCloseClick = () => {
    setConfirmDialogOpen(true);
  };

  const handleConfirmClose = () => {
    onClosePosition(position);
    setConfirmDialogOpen(false);
  };

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case "open":
        return <Badge className="bg-green-500">{t("status.active")}</Badge>;
      case "pending":
        return <Badge className="bg-yellow-500">{t("status.pending")}</Badge>;
      case "closed":
        return <Badge className="bg-gray-500">{t("status.closed")}</Badge>;
      case "error":
        return <Badge className="bg-red-500">{t("status.error")}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  return (
    <>
      <Dialog open={isOpen} onOpenChange={onClose}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogTitle className="sr-only">{t("positionDetails")}</DialogTitle>
          <DialogDescription className="sr-only">
            {t("positionDetailsDescription", { pair: position.pool_pair })}
          </DialogDescription>
          <CardHeader className="pb-0">
            <CardTitle className="text-xl">
              {position.identifier || `AP${position.id}`}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6 pt-4">
            <div className="grid grid-cols-2 gap-x-8 gap-y-4">
              <div>
                <div className="text-muted-foreground text-sm">{t("pair")}</div>
                <div className="font-medium">{position.pool_pair}</div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("openTime")}
                </div>
                <div className="font-medium">
                  {new Date(position.created_at * 1000).toLocaleString()}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("status.title")}
                </div>
                <div className="font-medium">
                  {renderStatusBadge(position.status)}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">{t("apr")}</div>
                <div className="font-medium text-green-500">
                  {position.apr
                    ? `${Number(position.apr).toFixed(2)}%`
                    : "0.00%"}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("initialAmount")}
                </div>
                <div className="font-medium">
                  {formatCurrency(Number(position.amount0_initial), "", {
                    showSymbol: false,
                  })}{" "}
                  {token0} -{" "}
                  {formatCurrency(Number(position.amount1_initial), "", {
                    showSymbol: false,
                  })}{" "}
                  {token1}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("currentAmount")}
                </div>
                <div className="font-medium">
                  {formatCurrency(Number(position.amount0), "", {
                    showSymbol: false,
                  })}{" "}
                  {token0} -{" "}
                  {formatCurrency(Number(position.amount1), "", {
                    showSymbol: false,
                  })}{" "}
                  {token1}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">{token0}</div>
                <div className="font-medium">
                  {formatCurrency(Number(position.amount0), "", {
                    showSymbol: false,
                  })}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">{token1}</div>
                <div className="font-medium">
                  {formatCurrency(Number(position.amount1), "", {
                    showSymbol: false,
                  })}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("collectedFees")}
                </div>
                <div className="font-medium">
                  {formatCurrency(
                    Number(position.fee_collected0) +
                      Number(position.fee_collected1),
                    "",
                    { showSymbol: false },
                  )}
                </div>
                <div className="text-xs text-muted-foreground">
                  {token0}:{" "}
                  {formatCurrency(Number(position.fee_collected0), "", {
                    showSymbol: false,
                  })}{" "}
                  & {token1}:{" "}
                  {formatCurrency(Number(position.fee_collected1), "", {
                    showSymbol: false,
                  })}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("uncollectedFees")}
                </div>
                <div className="font-medium">
                  {formatCurrency(feesValue, "", { showSymbol: false })}
                </div>
                <div className="text-xs text-muted-foreground">
                  {token0}:{" "}
                  {formatCurrency(Number(position.tokens_owed0), "", {
                    showSymbol: false,
                  })}{" "}
                  & {token1}:{" "}
                  {formatCurrency(Number(position.tokens_owed1), "", {
                    showSymbol: false,
                  })}
                </div>
              </div>

              {position.estimate_fee_token0 &&
                position.estimate_fee_token1 &&
                position.total_estimate_fee_in_token0 && (
                  <div>
                    <div className="text-muted-foreground text-sm">
                      {t("estimatedFees")}
                    </div>
                    <div className="font-medium">
                      {formatCurrency(
                        Number(position.total_estimate_fee_in_token0),
                        "",
                        {
                          showSymbol: false,
                        },
                      )}{" "}
                      {token0}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      {token0}:{" "}
                      {formatCurrency(
                        Number(position.estimate_fee_token0),
                        "",
                        {
                          showSymbol: false,
                        },
                      )}{" "}
                      & {token1}:{" "}
                      {formatCurrency(
                        Number(position.estimate_fee_token1),
                        "",
                        {
                          showSymbol: false,
                        },
                      )}
                    </div>
                  </div>
                )}

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("minPrice")}
                </div>
                <div className="font-medium">
                  {formatDisplayPrice(lowerPrice)} {token1}/{token0}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("maxPrice")}
                </div>
                <div className="font-medium">
                  {formatDisplayPrice(upperPrice)} {token1}/{token0}
                </div>
              </div>

              <div>
                <div className="text-muted-foreground text-sm">
                  {t("currentPrice")}
                </div>
                <div className="font-medium">
                  {formatDisplayPrice(currentPrice)} {token1}/{token0}
                </div>
              </div>
            </div>

            <div className="flex gap-4 justify-center mt-6">
              {isOpenStatus && (
                <>
                  <Button
                    className="flex-1"
                    variant="outline"
                    disabled={!canCollectFee}
                    onClick={() => onClaim(position.id)}
                  >
                    {t("getFee")}
                  </Button>
                  <Button className="flex-1" onClick={handleCloseClick}>
                    {t("close")}
                  </Button>
                </>
              )}
              {!isOpenStatus && (
                <Button className="flex-1" variant="outline" onClick={onClose}>
                  {t("closeDialog")}
                </Button>
              )}
            </div>
          </CardContent>
        </DialogContent>
      </Dialog>

      <ConfirmCloseDialog
        isOpen={confirmDialogOpen}
        position={position}
        onClose={() => setConfirmDialogOpen(false)}
        onConfirm={handleConfirmClose}
      />
    </>
  );
}
