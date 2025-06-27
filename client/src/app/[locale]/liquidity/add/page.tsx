"use client";

import { useState, useEffect, Suspense, useCallback } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useRouter, useSearchParams } from "next/navigation";
import { fetchPoolByPair, FormattedPool } from "@/lib/api/pools";
import { useTranslations } from "next-intl";
import { useWallet } from "@/hooks/use-wallet";
import { NewPosition } from "./new-position/new";

// Extracted component that uses searchParams
function AddLiquidityContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const poolParam = searchParams.get("pool");
  const t = useTranslations();
  const { data: walletData } = useWallet();

  const [pool, setPool] = useState<FormattedPool | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const getTokenBalance = useCallback(
    (token: string): number => {
      if (!token || !walletData) return 0;

      try {
        const coinAccountBalance = walletData.coin_accounts.find(
          (account) =>
            account.coin_currency.toLowerCase() === token.toLowerCase(),
        )?.balance;
        const fiatAccountBalance = walletData.fiat_accounts.find(
          (account) => account.currency.toLowerCase() === token.toLowerCase(),
        )?.balance;
        return coinAccountBalance || fiatAccountBalance || 0;
      } catch (error) {
        console.error(`Error getting balance for ${token}:`, error);
        return 0;
      }
    },
    [walletData],
  );

  // Fetch pool details from API
  useEffect(() => {
    const loadPoolDetails = async () => {
      if (!poolParam) {
        setError(t("liquidity.noPositionsFound"));
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const poolData = await fetchPoolByPair(poolParam);

        if (!poolData) {
          setError(t("liquidity.noPositionsFound"));
          setLoading(false);
          return;
        }

        setPool(poolData);
        setError(null);
      } catch (err) {
        console.error("Failed to load pool details:", err);
        setError(t("common.errors.failedToLoad"));
      } finally {
        setLoading(false);
      }
    };

    loadPoolDetails();
  }, [poolParam, t]);

  // Prevent redirect if poolParam exists but was lost on refresh
  useEffect(() => {
    if (!poolParam && !loading && sessionStorage.getItem("lastPoolParam")) {
      const lastPool = sessionStorage.getItem("lastPoolParam");
      router.push(`/liquidity/add?pool=${lastPool}`);
    } else if (poolParam) {
      sessionStorage.setItem("lastPoolParam", poolParam);
    }
  }, [poolParam, router, loading]);

  // Redirect if no pool is selected or pool not found
  useEffect(() => {
    if (!poolParam && !sessionStorage.getItem("lastPoolParam")) {
      router.push("/liquidity/pools");
    }
  }, [poolParam, router]);

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div
          data-testid="loading-spinner"
          className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"
        ></div>
      </div>
    );
  }

  if (error || !pool) {
    return (
      <Card>
        <CardContent className="py-8">
          <div className="text-center text-destructive">
            {error || t("liquidity.noPositionsFound")}
          </div>
          <div className="flex justify-center mt-4">
            <Button onClick={() => router.push("/liquidity/pools")}>
              {t("liquidity.selectPool")}
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return <NewPosition pool={pool} getTokenBalance={getTokenBalance} />;
}

// Original default export now wraps the content component with Suspense
export default function AddLiquidity() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <AddLiquidityContent />
    </Suspense>
  );
}
