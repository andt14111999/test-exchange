import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

// Định nghĩa lại interface tương thích với WalletData từ use-wallet.ts
export interface CoinAccount {
  id: number;
  user_id: number;
  coin_currency: string;
  balance: number;
  frozen_balance: number;
  created_at: string;
  updated_at: string;
}

export interface FiatAccount {
  id: number;
  user_id: number;
  currency: string;
  balance: number;
  frozen_balance: number;
  created_at: string;
  updated_at: string;
}

export interface WalletData {
  coin_accounts: CoinAccount[];
  fiat_accounts: FiatAccount[];
}

export interface CoinsResponse {
  coins: string[];
  fiats: string[];
}

export interface CoinLayerSetting {
  layer: string;
  deposit_enabled: boolean;
  withdraw_enabled: boolean;
  swap_enabled: boolean;
  maintenance: boolean;
}

export interface CoinSetting {
  id: number;
  currency: string;
  deposit_enabled: boolean;
  withdraw_enabled: boolean;
  swap_enabled: boolean;
  layers: CoinLayerSetting[];
  created_at: string;
  updated_at: string;
}

/**
 * Hàm lấy danh sách coins và fiats từ API
 * @returns Danh sách coins và fiats
 */
export const fetchCoins = async (): Promise<CoinsResponse> => {
  try {
    const response = await apiClient.get<CoinsResponse>(API_ENDPOINTS.coins);
    return response.data;
  } catch (error) {
    console.error("Error fetching coins:", error);
    // Trả về dữ liệu mặc định nếu API gặp lỗi
    return {
      coins: ["usdt", "btc", "eth"],
      fiats: ["vnd", "php", "ngn"],
    };
  }
};

/**
 * Kiểm tra xem một token có phải là coin hay không
 * @param token Token cần kiểm tra
 * @param coinsData Dữ liệu coins đã lấy từ API
 * @returns true nếu token là coin, false nếu là fiat
 */
export const isCoin = (token: string, coinsData: CoinsResponse): boolean => {
  return coinsData.coins.includes(token.toLowerCase());
};

/**
 * Lấy số dư của một token cụ thể từ dữ liệu balance
 * @param token Token cần lấy số dư
 * @param walletData Dữ liệu wallet của user
 * @param coinsData Dữ liệu coins đã lấy từ API
 * @returns Số dư của token, hoặc 0 nếu không tìm thấy
 */
export const getTokenBalance = (
  token: string,
  walletData: WalletData | undefined,
  coinsData: CoinsResponse,
): string => {
  if (!walletData) return "0";

  const tokenLower = token.toLowerCase();

  if (isCoin(tokenLower, coinsData)) {
    // Tìm trong coin_accounts
    const account = walletData.coin_accounts.find(
      (acc: CoinAccount) => acc.coin_currency.toLowerCase() === tokenLower,
    );
    return account ? account.balance.toString() : "0";
  } else {
    // Tìm trong fiat_accounts
    const account = walletData.fiat_accounts.find(
      (acc: FiatAccount) => acc.currency.toLowerCase() === tokenLower,
    );
    return account ? account.balance.toString() : "0";
  }
};

export const fetchCoinSettings = async (): Promise<CoinSetting[]> => {
  try {
    const response = await apiClient.get<CoinSetting[]>(
      API_ENDPOINTS.settings.coinSettings,
    );
    if (!response.data || response.data.length === 0) {
      console.warn("No coin settings returned from API, using defaults");
      return getDefaultCoinSettings();
    }
    return response.data;
  } catch (error) {
    console.error("Error fetching coin settings:", error);
    return getDefaultCoinSettings();
  }
};

function getDefaultCoinSettings(): CoinSetting[] {
  // Default settings for common currencies
  const defaultSettings: CoinSetting[] = [
    {
      id: 0,
      currency: "NGN",
      deposit_enabled: true,
      withdraw_enabled: true,
      swap_enabled: false,
      layers: [],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    },
    {
      id: 1,
      currency: "VND",
      deposit_enabled: true,
      withdraw_enabled: true,
      swap_enabled: false,
      layers: [],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    },
    {
      id: 2,
      currency: "PHP",
      deposit_enabled: true,
      withdraw_enabled: true,
      swap_enabled: false,
      layers: [],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    },
  ];
  return defaultSettings;
}
