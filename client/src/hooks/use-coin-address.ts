import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchCoinAddress, generateCoinAddress } from "@/lib/api/coin-address";

interface UseCoinAddressParams {
  coinCurrency: string;
  layer: string;
}

export function useCoinAddress({ coinCurrency, layer }: UseCoinAddressParams) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["coinAddress", coinCurrency, layer],
    queryFn: () => fetchCoinAddress(coinCurrency, layer),
  });

  const generateMutation = useMutation({
    mutationFn: () => generateCoinAddress(coinCurrency, layer),
    onSuccess: () => {
      // Invalidate and refetch the address query after successful generation
      queryClient.invalidateQueries({
        queryKey: ["coinAddress", coinCurrency, layer],
      });
    },
  });

  return {
    ...query,
    generateAddress: generateMutation.mutate,
    isGenerating: generateMutation.isPending,
    generateError: generateMutation.error,
  };
}
