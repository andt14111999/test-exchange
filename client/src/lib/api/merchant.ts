import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface MerchantRegistrationResponse {
  status: string;
  data: {
    id: number;
    email: string;
    role: string;
  };
}

export interface Escrow {
  id: number;
  usdt_amount: string;
  fiat_amount: string;
  fiat_currency: string;
  status: string;
  created_at: string;
  updated_at: string;
}

export interface ApiResponse<T> {
  status?: string;
  data?: T | T[];
  message?: string;
}

export interface Offer {
  id: number;
  offer_type: "buy" | "sell";
  coin_currency: string;
  currency: string;
  price: number | string;
  total_amount: number | string;
  available_amount?: number | string;
  min_amount: number | string;
  max_amount: number | string;
  payment_method_id: number;
  payment_time: number;
  payment_details: PaymentDetails | string;
  country_code: string;
  is_active?: boolean;
  online?: boolean;
  status?: string;
  disabled?: boolean;
  deleted?: boolean;
  created_at: string;
  updated_at?: string;
  merchant_display_name?: string;
  user_id?: number;
}

export interface PaymentDetails {
  bank_name?: string;
  bank_account_number?: string;
  bank_account_name?: string;
  bank_id?: string | number;
  payment_instructions?: string;
  [key: string]: string | number | boolean | undefined;
}

export interface CreateOfferRequest {
  offer_type: "buy" | "sell";
  coin_currency: string;
  currency: string;
  price: number;
  total_amount: number;
  min_amount: number;
  max_amount: number;
  payment_method_id: number;
  payment_time: number;
  payment_details: PaymentDetails;
  country_code: string;
  is_active?: boolean;
}

export interface UpdateOfferRequest extends Partial<CreateOfferRequest> {
  is_active?: boolean;
}

/**
 * Register current user as a merchant
 */
export async function registerAsMerchant(): Promise<MerchantRegistrationResponse> {
  try {
    const response = await apiClient.post<MerchantRegistrationResponse>(
      API_ENDPOINTS.merchant.register,
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Get all escrows for the current merchant
 */
export async function getEscrows(): Promise<ApiResponse<Escrow> | Escrow[]> {
  try {
    const response = await apiClient.get(API_ENDPOINTS.merchant.escrows.list);
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Get a specific escrow by ID
 */
export async function getEscrow(id: number): Promise<ApiResponse<Escrow>> {
  try {
    const response = await apiClient.get<ApiResponse<Escrow>>(
      API_ENDPOINTS.merchant.escrows.get(id),
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Create a new escrow
 */
export async function createEscrow(
  usdtAmount: string,
  fiatCurrency: string,
): Promise<ApiResponse<Escrow>> {
  try {
    const response = await apiClient.post<ApiResponse<Escrow>>(
      API_ENDPOINTS.merchant.escrows.create,
      {
        usdt_amount: usdtAmount,
        fiat_currency: fiatCurrency,
      },
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Cancel an existing escrow
 */
export async function cancelEscrow(id: number): Promise<ApiResponse<Escrow>> {
  try {
    const response = await apiClient.post<ApiResponse<Escrow>>(
      API_ENDPOINTS.merchant.escrows.cancel(id),
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Get all offers for the current merchant
 */
export async function getOffers(): Promise<ApiResponse<Offer> | Offer[]> {
  try {
    const response = await apiClient.get(API_ENDPOINTS.merchant.offers.list);
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Get a specific offer by ID
 */
export async function getOffer(id: number): Promise<ApiResponse<Offer>> {
  try {
    const url = API_ENDPOINTS.merchant.offers.get(id);
    const response = await apiClient.get<ApiResponse<Offer>>(url);

    // Check if the response is valid
    if (!response) {
      return {
        status: "error",
        data: {} as Offer,
        message: "Failed to fetch offer data",
      };
    }

    // Handle case where API returns data directly without wrapping in a data field
    if (
      response.data &&
      typeof response.data === "object" &&
      "id" in response.data
    ) {
      // Response is directly the offer object
      return {
        status: "success",
        data: response.data as unknown as Offer,
      };
    }

    return response.data;
  } catch (error) {
    return {
      status: "error",
      data: {} as Offer,
      message: error instanceof Error ? error.message : "Unknown error",
    };
  }
}

/**
 * Create a new offer
 */
export async function createOffer(
  offerData: CreateOfferRequest,
): Promise<ApiResponse<Offer>> {
  try {
    const response = await apiClient.post<ApiResponse<Offer>>(
      API_ENDPOINTS.merchant.offers.create,
      offerData,
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Update an existing offer
 */
export async function updateOffer(
  id: number,
  offerData: UpdateOfferRequest,
): Promise<ApiResponse<Offer>> {
  try {
    const response = await apiClient.put<ApiResponse<Offer>>(
      API_ENDPOINTS.merchant.offers.update(id),
      offerData,
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Delete an offer
 */
export async function deleteOffer(id: number): Promise<ApiResponse<Offer>> {
  try {
    const response = await apiClient.delete<ApiResponse<Offer>>(
      API_ENDPOINTS.merchant.offers.delete(id),
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Enable an offer
 */
export async function enableOffer(id: number): Promise<ApiResponse<Offer>> {
  try {
    const endpoint = API_ENDPOINTS.merchant.offers.enable(id);
    const response = await apiClient.put<ApiResponse<Offer>>(endpoint);
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Disable an offer
 */
export async function disableOffer(id: number): Promise<ApiResponse<Offer>> {
  try {
    const endpoint = API_ENDPOINTS.merchant.offers.disable(id);
    const response = await apiClient.put<ApiResponse<Offer>>(endpoint);
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Set offer online/offline status
 */
export async function setOfferOnlineStatus(
  id: number,
  online: boolean,
): Promise<ApiResponse<Offer>> {
  try {
    const endpoint = API_ENDPOINTS.merchant.offers.setOnlineStatus(id);
    const response = await apiClient.put<ApiResponse<Offer>>(endpoint, {
      online,
    });
    return response.data;
  } catch (error) {
    throw error;
  }
}

/**
 * Get all offers for the current merchant (including disabled ones)
 */
export async function getMerchantOffers(): Promise<
  ApiResponse<Offer> | Offer[]
> {
  try {
    const response = await apiClient.get(
      API_ENDPOINTS.merchant.offers.merchantList,
    );
    return response.data;
  } catch (error) {
    throw error;
  }
}
