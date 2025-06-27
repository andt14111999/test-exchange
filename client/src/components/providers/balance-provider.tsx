"use client";

import { useBalanceChannel } from "@/hooks/use-balance-channel";
import { useWallet } from "@/hooks/use-wallet";
import type { BalanceData, BalanceResponse } from "@/lib/api/balance";
import { useBalanceStore } from "@/lib/store/balance-store";
import { useUserStore } from "@/lib/store/user-store";
import { useQueryClient } from "@tanstack/react-query";

/**
 * BalanceProvider component
 *
 * Initializes the balance data and sets up WebSocket connection for real-time updates.
 * This makes balance data available throughout the app via the balance store.
 */
export function BalanceProvider({ children }: { children: React.ReactNode }) {
  useWallet();

  const user = useUserStore((state) => state.user);
  const userId = user?.id ? Number(user.id) : 0;
  const setBalanceData = useBalanceStore((state) => state.setBalanceData);
  const setBalanceUpdated = useBalanceStore((state) => state.setBalanceUpdated);
  const queryClient = useQueryClient();

  useBalanceChannel({
    userId,
    onBalanceUpdate: (balanceData: BalanceData) => {
      console.log(`💰 Balance updated for user ${userId}:`, balanceData);
      setBalanceData(balanceData);

      queryClient.setQueryData<BalanceResponse>(
        ["wallet", user?.id],
        (oldData: BalanceResponse | undefined) => {
          if (!oldData)
            return {
              status: "success",
              data: balanceData,
            };

          return {
            ...oldData,
            data: balanceData,
          };
        },
      );

      setBalanceUpdated(true);

      setTimeout(() => {
        setBalanceUpdated(false);
      }, 1000);
    },
  });

  return <>{children}</>;
}
