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
  short_name: string;
  support: number;
  isTransfer: number;
  swift_code: string;
}

export interface GetBanksResponse {
  status: string;
  data: Bank[];
}

export const getBanks = async (): Promise<GetBanksResponse> => {
  try {
    const response = await apiClient.get<GetBanksResponse>(API_ENDPOINTS.banks);
    return response.data;
  } catch (error) {
    throw error;
  }
};
