export interface CoinAddressResponse {
  status: string;
  data: {
    address: string;
    network: string;
    coin_currency: string;
  };
}
