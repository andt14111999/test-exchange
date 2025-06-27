import { useQuery, useMutation } from "@tanstack/react-query";
import { apiClient } from "../client";
import { queryClient } from "@/lib/query/query-client";

export function useWalletBalance() {
  return useQuery({
    queryKey: ["wallet", "balance"],
    queryFn: () => apiClient.get("/wallet/balance"),
  });
}

export function useTransferFunds() {
  return useMutation({
    mutationFn: (data: { to: string; amount: string; currency: string }) =>
      apiClient.post("/wallet/transfer", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
    },
  });
}
