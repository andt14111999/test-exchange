"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useEffect, useState } from "react";
import { fetchPools, FormattedPool } from "@/lib/api/pools";
import { useTranslations } from "next-intl";
import { useAmmPools } from "@/providers/amm-pool-provider";

export default function Pools() {
  const router = useRouter();
  const t = useTranslations("liquidity");
  const { getAllPools, updatePool } = useAmmPools();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initial load of pools
  useEffect(() => {
    const loadPools = async () => {
      try {
        setLoading(true);
        const poolsData = await fetchPools();
        // Cập nhật pools vào provider
        poolsData.forEach((pool) => updatePool(pool));
        setError(null);
      } catch (err) {
        console.error("Failed to load pools:", err);
        setError(t("common.errors.failedToLoad"));
      } finally {
        setLoading(false);
      }
    };

    loadPools();
  }, [t, updatePool]);

  const pools = getAllPools();

  // Helper function for currency formatting (consistent with positions page)
  const formatCurrency = (value: number | string) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      maximumFractionDigits: 0, // Match TVL formatting on pools page
    }).format(Number(value));
  };

  const handleItemClick = (pool: FormattedPool) => {
    router.push(`/liquidity/add?pool=${pool.pair}`);
  };

  return (
    <div className="container max-w-4xl mx-auto py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">{t("availablePools")}</h1>
        <Button
          variant="outline"
          onClick={() => router.push("/liquidity/positions")}
        >
          {t("myPositions")}
        </Button>
      </div>

      {/* Pools List Header (visible sm and up) */}
      <div className="hidden sm:grid sm:grid-cols-4 gap-4 items-center px-4 py-2 text-sm text-muted-foreground font-medium mb-2">
        <div className="col-span-1 min-w-0">{t("pool")}</div>
        <div className="text-left min-w-0">{t("apr")}</div>
        <div className="text-left min-w-0">{t("feeTier")}</div>
        <div className="text-left min-w-0">{t("tvl")}</div>
      </div>

      {/* Loading state */}
      {loading && (
        <div className="flex justify-center items-center py-12">
          <div
            data-testid="loading-spinner"
            className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"
          ></div>
        </div>
      )}

      {/* Error state */}
      {error && (
        <div className="bg-destructive/10 text-destructive p-4 rounded-md mb-4">
          {error}
        </div>
      )}

      {/* Pools List - Using Cards styled as table rows */}
      {!loading && !error && (
        <div className="space-y-3">
          {pools.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              {t("noPoolsAvailable")}
            </div>
          ) : (
            pools.map((pool) => (
              <Card
                key={pool.id}
                onClick={() => handleItemClick(pool)}
                className="cursor-pointer hover:bg-muted/50 transition-colors overflow-hidden"
              >
                {/* Grid adjusts from mobile layout to 4 cols (desktop) */}
                <CardContent className="p-4 grid grid-cols-3 gap-y-2 gap-x-4 sm:grid-cols-4 sm:items-center">
                  {/* Pool Name: Full width on mobile, Col 1 on desktop */}
                  <div className="col-span-3 sm:col-span-1 font-medium min-w-0 truncate">
                    {pool.name}
                  </div>

                  {/* APR: Col 1 (row 2) on mobile, Col 2 on desktop */}
                  <div className="col-span-1 sm:col-span-1 text-left min-w-0">
                    <div className="text-xs text-muted-foreground font-medium mb-1 sm:hidden">
                      {t("apr")}
                    </div>
                    <span className="text-green-600">{pool.apr}%</span>
                  </div>

                  {/* Fee Tier: Col 2 (row 2) on mobile, Col 3 on desktop */}
                  <div className="col-span-1 sm:col-span-1 text-left min-w-0">
                    <div className="text-xs text-muted-foreground font-medium mb-1 sm:hidden">
                      {t("feeTier")}
                    </div>
                    {pool.fee * 100}%
                  </div>

                  {/* TVL: Col 3 (row 2) on mobile, Col 4 on desktop */}
                  <div className="col-span-1 sm:col-span-1 text-left min-w-0">
                    <div className="text-xs text-muted-foreground font-medium mb-1 sm:hidden">
                      {t("tvl")}
                    </div>
                    {formatCurrency(pool.liquidity.toNumber())}
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}
    </div>
  );
}
