"use client";

import { useEffect, useState } from "react";
import { useAMMStore } from "@/lib/amm/store";

const INITIAL_POOLS = [
  {
    id: "usdt-vnd",
    token0: "USDT",
    token1: "VND",
    initialPrice: "23500", // 1 USDT = 23,500 VND
    initialLiquidity: "1000000", // 1M USDT worth of liquidity
  },
  {
    id: "usdt-php",
    token0: "USDT",
    token1: "PHP",
    initialPrice: "56", // 1 USDT = 56 PHP
    initialLiquidity: "1000000",
  },
  {
    id: "usdt-ngn",
    token0: "USDT",
    token1: "NGN",
    initialPrice: "1550", // 1 USDT = 1,550 NGN
    initialLiquidity: "1000000",
  },
];

export function PoolInitializer() {
  const [isClient, setIsClient] = useState(false);
  const initializePool = useAMMStore((state) => state.initializePool);

  useEffect(() => {
    setIsClient(true);
  }, []);

  useEffect(() => {
    if (!isClient) return;

    // Initialize pools if they don't exist
    INITIAL_POOLS.forEach((pool) => {
      initializePool(
        pool.id,
        pool.token0,
        pool.token1,
        pool.initialPrice,
        pool.initialLiquidity,
      );
    });
  }, [initializePool, isClient]);

  return null; // This component doesn't render anything
}
