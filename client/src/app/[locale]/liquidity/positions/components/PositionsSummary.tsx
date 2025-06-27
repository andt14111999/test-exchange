import { memo } from "react";
import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AmmPosition } from "@/lib/api/positions";
import { formatCurrency, formatLiquidity } from "@/lib/utils/format";

interface PositionsSummaryProps {
  positions: AmmPosition[];
  onClaimAll: () => void;
}

const PositionsSummary = memo(
  ({ positions, onClaimAll }: PositionsSummaryProps) => {
    const t = useTranslations("liquidity");

    // Tính tổng giá trị thanh khoản
    const totalLiquidity = positions.reduce(
      (sum, pos) => sum + Number(pos.liquidity),
      0,
    );

    // Tính tổng phí có thể nhận
    const totalFeesValue = positions.reduce(
      (sum, pos) => sum + (Number(pos.tokens_owed0) + Number(pos.tokens_owed1)),
      0,
    );

    return (
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 md:gap-8 items-end">
            <div>
              <h2 className="text-sm text-muted-foreground mb-1">
                {t("totalPositions")}
              </h2>
              <div className="text-2xl font-bold">
                {formatLiquidity(totalLiquidity)}
              </div>
            </div>
            <div>
              <h2 className="text-sm text-muted-foreground mb-1">
                {t("claimableFees")}
              </h2>
              <div className="text-2xl font-bold">
                {formatCurrency(totalFeesValue)}
              </div>
            </div>
            <div className="flex justify-start md:justify-end">
              <Button
                className="w-full sm:w-32 mt-2 md:mt-0"
                disabled={totalFeesValue === 0}
                onClick={onClaimAll}
              >
                {t("claimAll")}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  },
);

PositionsSummary.displayName = "PositionsSummary";

export default PositionsSummary;
