import { useAMMStore } from "@/lib/amm/store";
import { useMutation, useQuery } from "@tanstack/react-query";
import { apiClient } from "../client";
import { queryClient } from "@/lib/query/query-client";
import { POOLS } from "@/lib/amm/constants";

export function usePools() {
  const initializePool = useAMMStore((state) => state.initializePool);

  return useQuery({
    queryKey: ["pools"],
    queryFn: async () => {
      // Fetch pools from API
      const response = await apiClient.get("/pools");
      const poolsData = response.data;

      // Initialize pools in AMM store
      POOLS.forEach((pool) => {
        const poolData = poolsData[pool.id];
        if (poolData) {
          initializePool(
            pool.id,
            pool.token0.name,
            pool.token1.name,
            poolData.price,
            poolData.liquidity,
          );
        }
      });

      return poolsData;
    },
    retry: 3,
    retryDelay: 1000,
  });
}

export function useCreatePool() {
  return useMutation({
    mutationFn: (data: {
      token0: string;
      token1: string;
      initialPrice: string;
      initialLiquidity: string;
    }) => apiClient.post("/pools", data),
    onSuccess: () => {
      // Invalidate and refetch pools after mutation
      queryClient.invalidateQueries({ queryKey: ["pools"] });
    },
  });
}
