"use client";

import { FormattedPool } from "@/lib/api/pools";
import { useTranslations } from "next-intl";
import { ArrowUpDown } from "lucide-react";
import { TickMath } from "@/lib/amm/tick-math";
import { useState } from "react";

interface PoolInfoProps {
  pool: FormattedPool;
}

export function PoolInfo({ pool }: PoolInfoProps) {
  const t = useTranslations();
  const [isPriceInverted, setIsPriceInverted] = useState(false);

  const handleReverseClick = () => {
    setIsPriceInverted(!isPriceInverted);
  };

  const currentPrice = TickMath.tickToPrice(pool.currentTick);

  const displayPrice = isPriceInverted
    ? (1 / currentPrice).toFixed(8)
    : TickMath.formatPrice(currentPrice);

  const displayPair = isPriceInverted
    ? `${pool.pair.split("_")[1]}/${pool.pair.split("_")[0]}`
    : `${pool.pair.split("_")[0]}/${pool.pair.split("_")[1]}`;

  return (
    <div className="space-y-2">
      <h3 className="text-lg font-medium">{t("liquidity.selectedPool")}</h3>
      <div className="flex justify-between items-center p-3 bg-muted rounded-md">
        <div>
          <div className="font-medium">{pool.pair}</div>
          <div className="text-sm text-muted-foreground">
            {t("liquidity.fee")}: {pool.fee}%
          </div>
        </div>
        <div className="text-sm flex flex-col items-end">
          <div className="flex items-center justify-between mt-2">
            <div className="flex items-center">
              <span className="text-sm mr-2">
                {displayPair}: {displayPrice}
              </span>
              <button
                className="p-1 rounded bg-primary text-primary-foreground"
                onClick={handleReverseClick}
                id="reverseButton"
              >
                <ArrowUpDown size={14} />
              </button>
            </div>
          </div>
          <div>
            {t("liquidity.currentTick")}: {pool.currentTick}
          </div>
        </div>
      </div>
    </div>
  );
}
