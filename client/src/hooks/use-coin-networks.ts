import { useQuery } from "@tanstack/react-query";
import { fetchCoinSettings, type CoinSetting } from "@/lib/api/coins";

export interface Network {
  id: string;
  name: string;
  enabled: boolean;
}

const NETWORK_NAMES: Record<string, string> = {
  bep20: "BNB Smart Chain (BEP20)",
  trc20: "TRON (TRC20)",
  erc20: "Ethereum (ERC20)",
  bitcoin: "Bitcoin (BTC)",
  solana: "Solana",
};

export function useCoinNetworks(coin: string) {
  const { data: coinSettings, isLoading } = useQuery<CoinSetting[]>({
    queryKey: ["coin-settings"],
    queryFn: fetchCoinSettings,
  });

  const networks =
    coinSettings
      ?.find((s) => s.currency.toLowerCase() === coin.toLowerCase())
      ?.layers.map((layer) => ({
        id: layer.layer.toLowerCase(),
        name: NETWORK_NAMES[layer.layer.toLowerCase()] || layer.layer,
        enabled: layer.deposit_enabled,
      })) || [];

  return {
    networks,
    isLoading,
  };
}
