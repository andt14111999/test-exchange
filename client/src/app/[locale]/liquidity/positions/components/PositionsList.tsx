import { memo } from "react";
import { useTranslations } from "next-intl";
import { AmmPosition } from "@/lib/api/positions";
import PositionItem from "./PositionItem";

interface PositionsListProps {
  positions: AmmPosition[];
  onClaim: (id: number) => void;
  onClose: (position: AmmPosition) => void;
  onViewDetail: (position: AmmPosition) => void;
}

const PositionsList = memo(
  ({ positions, onClaim, onClose, onViewDetail }: PositionsListProps) => {
    const t = useTranslations("liquidity");

    if (positions.length === 0) {
      return (
        <div className="text-center p-12 border rounded-lg bg-muted/10">
          <h3 className="text-lg font-medium mb-2">{t("noPositionsFound")}</h3>
        </div>
      );
    }

    return (
      <div className="space-y-4">
        <div className="hidden lg:grid lg:grid-cols-6 gap-4 items-center px-4 py-2 text-sm text-muted-foreground font-medium">
          <div className="col-span-1 min-w-0">{t("pair")}</div>
          <div className="col-span-1 min-w-0">{t("range")}</div>
          <div className="text-left min-w-0">{t("token0")}</div>
          <div className="text-left min-w-0">{t("token1")}</div>
          <div className="text-center min-w-0">{t("status.title")}</div>
          <div className="text-center min-w-0">{t("actions")}</div>
        </div>

        {positions.map((position) => (
          <PositionItem
            key={position.id}
            position={position}
            onClaim={onClaim}
            onClose={onClose}
            onViewDetail={onViewDetail}
          />
        ))}
      </div>
    );
  },
);

PositionsList.displayName = "PositionsList";

export default PositionsList;
