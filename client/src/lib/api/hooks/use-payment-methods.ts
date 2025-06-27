import { useQuery, UseQueryResult } from "@tanstack/react-query";
import {
  getPaymentMethods,
  getPaymentMethod,
  PaymentMethod,
  PaymentMethodResponse,
} from "../payment-methods";

// Hook for fetching all payment methods
export function usePaymentMethods(
  countryCode?: string,
  enabled?: boolean,
): UseQueryResult<PaymentMethodResponse | PaymentMethod[], Error> {
  return useQuery({
    queryKey: ["paymentMethods", { countryCode, enabled }],
    queryFn: () => getPaymentMethods(countryCode, enabled),
  });
}

// Hook for fetching a single payment method
export function usePaymentMethod(
  id?: number,
): UseQueryResult<PaymentMethodResponse, Error> {
  return useQuery({
    queryKey: ["paymentMethod", id],
    queryFn: () => {
      if (!id) throw new Error("Payment method ID is required");
      return getPaymentMethod(id);
    },
    enabled: !!id,
  });
}
