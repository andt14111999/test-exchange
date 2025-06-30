import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface Bank {
  name: string;
  code: string;
  bin: string;
  shortName: string;
  logo: string;
  transferSupported: number;
  lookupSupported: number;
  support: number;
  isTransfer: number;
  swiftCode: string;
  countryCode: string;
  countryName: string;
}

export interface GetBanksResponse {
  status: string;
  data: Bank[];
}

export const getBanks = async (countryCode?: string): Promise<GetBanksResponse> => {
  try {
    const response = await apiClient.get<GetBanksResponse>(API_ENDPOINTS.banks, {
      params: {
        country_code: countryCode,
      },
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};
