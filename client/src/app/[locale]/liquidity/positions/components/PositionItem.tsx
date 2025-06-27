import { memo, useState } from "react";
import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AmmPosition } from "@/lib/api/positions";
import { TickMath } from "@/lib/amm/tick-math";
import StatusBadge from "./StatusBadge";
import { formatCurrency } from "@/lib/utils/format";
import ConfirmCloseDialog from "./ConfirmCloseDialog";

interface PositionItemProps {
  position: AmmPosition;
  onClaim: (id: number) => void;
  onClose: (position: AmmPosition) => void;
  onViewDetail: (position: AmmPosition) => void;
}

const PositionItem = memo(
  ({ position, onClaim, onClose, onViewDetail }: PositionItemProps) => {
    const t = useTranslations("liquidity");
    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const feesValue =
      Number(position.tokens_owed0) + Number(position.tokens_owed1);
    const isOpenStatus = position.status === "open";
    // Check if total_estimate_fee_in_token0 is greater than 1 for collect fee button
    const canCollectFee = Number(position.total_estimate_fee_in_token0) > 1;

    // Tính giá từ tick index sử dụng TickMath
    const lowerPrice = TickMath.tickToPrice(position.tick_lower_index);
    const upperPrice = TickMath.tickToPrice(position.tick_upper_index);

    // Tách cặp để lấy tên token
    const [token0, token1] = position.pool_pair.split("/");

    const handleButtonClick =
      <T,>(callback: (arg: T) => void, data: T) =>
      (e: React.MouseEvent) => {
        e.stopPropagation();
        callback(data);
      };

    const handleCloseClick = (e: React.MouseEvent) => {
      e.stopPropagation();
      setConfirmDialogOpen(true);
    };

    const handleConfirmClose = () => {
      onClose(position);
      setConfirmDialogOpen(false);
    };

    return (
      <>
        <Card
          className="overflow-hidden cursor-pointer hover:bg-accent/5 transition-colors"
          onClick={() => onViewDetail(position)}
        >
          <CardContent className="p-0">
            {/* Mobile View */}
            <div className="lg:hidden p-4 space-y-4">
              <div className="flex justify-between items-center">
                <div className="font-medium">{position.pool_pair}</div>
                <StatusBadge status={position.status} />
              </div>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <div className="text-muted-foreground">{t("range")}</div>
                  <div className="flex flex-col">
                    <span>{formatCurrency(lowerPrice)}</span>
                    <span>{formatCurrency(upperPrice)}</span>
                  </div>
                </div>
                <div>
                  <div className="text-muted-foreground">{token0}</div>
                  <div>
                    {formatCurrency(
                      Number(position.amount0) === 0
                        ? Number(position.amount0_initial)
                        : Number(position.amount0),
                    )}
                  </div>
                </div>
                <div>
                  <div className="text-muted-foreground">{token1}</div>
                  <div>
                    {formatCurrency(
                      Number(position.amount1) === 0
                        ? Number(position.amount1_initial)
                        : Number(position.amount1),
                    )}
                  </div>
                </div>
                <div>
                  <div className="text-muted-foreground">{t("fees")}</div>
                  <div>{formatCurrency(feesValue)}</div>
                </div>
              </div>
              {isOpenStatus && (
                <div
                  className="flex gap-2 mt-4"
                  onClick={(e) => e.stopPropagation()}
                >
                  <Button
                    size="sm"
                    className="flex-1"
                    variant="outline"
                    disabled={!canCollectFee}
                    onClick={handleButtonClick(onClaim, position.id)}
                  >
                    {t("getFee")}
                  </Button>
                  <Button
                    size="sm"
                    className="flex-1"
                    variant="outline"
                    onClick={handleCloseClick}
                  >
                    {t("close")}
                  </Button>
                </div>
              )}
            </div>

            {/* Desktop View */}
            <div className="hidden lg:grid lg:grid-cols-6 gap-4 items-center p-4">
              <div className="col-span-1 min-w-0 font-medium">
                {position.pool_pair}
              </div>
              <div className="col-span-1 min-w-0 flex flex-col">
                <span>{formatCurrency(lowerPrice)}</span>
                <span>{formatCurrency(upperPrice)}</span>
              </div>
              <div className="text-left min-w-0 flex flex-col">
                <span className="text-muted-foreground text-xs">{token0}</span>
                <span>
                  {formatCurrency(
                    Number(position.amount0) === 0
                      ? Number(position.amount0_initial)
                      : Number(position.amount0),
                  )}
                </span>
              </div>
              <div className="text-left min-w-0 flex flex-col">
                <span className="text-muted-foreground text-xs">{token1}</span>
                <span>
                  {formatCurrency(
                    Number(position.amount1) === 0
                      ? Number(position.amount1_initial)
                      : Number(position.amount1),
                  )}
                </span>
              </div>
              <div className="text-center min-w-0">
                <StatusBadge status={position.status} />
              </div>
              <div
                className="flex gap-2 justify-center"
                onClick={(e) => e.stopPropagation()}
              >
                {isOpenStatus ? (
                  <>
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={!canCollectFee}
                      onClick={handleButtonClick(onClaim, position.id)}
                    >
                      {t("getFee")}
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleCloseClick}
                    >
                      {t("close")}
                    </Button>
                  </>
                ) : (
                  <span className="text-sm text-muted-foreground">-</span>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        <ConfirmCloseDialog
          isOpen={confirmDialogOpen}
          position={position}
          onClose={() => setConfirmDialogOpen(false)}
          onConfirm={handleConfirmClose}
        />
      </>
    );
  },
);

PositionItem.displayName = "PositionItem";

export default PositionItem;
