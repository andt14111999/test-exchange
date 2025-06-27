"use client";

import React, { createContext, useContext, useState, useCallback } from "react";
import { useAmmPoolChannel } from "@/hooks/use-amm-pool-channel";
import type { ApiPool } from "@/lib/api/pools";
import { formatApiPool, type FormattedPool } from "@/lib/api/pools";

interface AmmPoolContextType {
  pools: Map<number, FormattedPool>;
  getPool: (id: number) => FormattedPool | undefined;
  getAllPools: () => FormattedPool[];
  updatePool: (pool: ApiPool) => void;
}

const AmmPoolContext = createContext<AmmPoolContextType | undefined>(undefined);

export function AmmPoolProvider({ children }: { children: React.ReactNode }) {
  const [pools, setPools] = useState<Map<number, FormattedPool>>(new Map());

  const updatePool = useCallback((poolData: ApiPool) => {
    setPools((prevPools) => {
      const newPools = new Map(prevPools);
      const formattedPool = formatApiPool(poolData);

      // Kiểm tra xem pool đã tồn tại chưa
      const existingPool = newPools.get(poolData.id);
      if (existingPool) {
        // Nếu đã tồn tại, chỉ cập nhật khi có thay đổi
        const hasChanges =
          JSON.stringify(existingPool) !== JSON.stringify(formattedPool);
        if (!hasChanges) {
          return prevPools; // Không có thay đổi, giữ nguyên state cũ
        }
        console.log(`🔄 Updated pool: ${formattedPool.name}`);
      } else {
        console.log(`➕ New pool added: ${formattedPool.name}`);
      }

      // Cập nhật hoặc thêm mới pool
      newPools.set(poolData.id, formattedPool);
      return newPools;
    });
  }, []);

  const handlePoolUpdate = useCallback(
    (poolData: ApiPool) => {
      updatePool(poolData);
    },
    [updatePool],
  );

  useAmmPoolChannel({
    onPoolUpdate: handlePoolUpdate,
  });

  const getPool = useCallback(
    (id: number) => {
      return pools.get(id);
    },
    [pools],
  );

  const getAllPools = useCallback(() => {
    return Array.from(pools.values());
  }, [pools]);

  const value = {
    pools,
    getPool,
    getAllPools,
    updatePool,
  };

  return (
    <AmmPoolContext.Provider value={value}>{children}</AmmPoolContext.Provider>
  );
}

export function useAmmPools() {
  const context = useContext(AmmPoolContext);
  if (context === undefined) {
    throw new Error("useAmmPools must be used within an AmmPoolProvider");
  }
  return context;
}
