import { CoinAddressResponse } from "@/types/coin-address";
import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export async function fetchCoinAddress(
  coinCurrency: string,
  layer: string,
): Promise<CoinAddressResponse> {
  const response = await apiClient.get(
    API_ENDPOINTS.coinAccounts.address(coinCurrency, layer),
  );
  return response.data;
}

export async function generateCoinAddress(
  coinCurrency: string,
  layer: string,
): Promise<CoinAddressResponse> {
  const response = await apiClient.post(
    API_ENDPOINTS.coinAccounts.generateAddress(coinCurrency, layer),
  );
  return response.data;
}
