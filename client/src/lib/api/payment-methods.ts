import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export interface PaymentMethod {
  id: number;
  name: string;
  display_name: string;
  description: string | null;
  country_code: string;
  enabled: boolean;
  icon_url: string | null;
  fields_required: {
    [key: string]: boolean;
  };
  created_at: string;
  updated_at: string;
}

export interface PaymentMethodResponse {
  status?: string;
  data?: PaymentMethod | PaymentMethod[];
  [index: number]: PaymentMethod;
  length?: number;
}

export async function getPaymentMethods(
  countryCode?: string,
  enabled?: boolean,
): Promise<PaymentMethodResponse | PaymentMethod[]> {
  try {
    let url = API_ENDPOINTS.paymentMethods.list;
    const params = new URLSearchParams();

    if (countryCode) {
      params.append("country_code", countryCode);
    }

    if (enabled !== undefined) {
      params.append("enabled", enabled.toString());
    }

    if (params.toString()) {
      url += `?${params.toString()}`;
    }

    const response = await apiClient.get(url);
    return response.data;
  } catch (error) {
    console.error("Failed to get payment methods:", error);
    throw error;
  }
}

export async function getPaymentMethod(
  id: number,
): Promise<PaymentMethodResponse> {
  try {
    const response = await apiClient.get<PaymentMethodResponse>(
      API_ENDPOINTS.paymentMethods.get(id),
    );
    return response.data;
  } catch (error) {
    console.error(`Failed to get payment method ${id}:`, error);
    throw error;
  }
}
