import { useQuery } from "@tanstack/react-query";
import { getOffer, getOffers, Offer, ApiResponse } from "../merchant";

// Key cho React Query
const OFFERS_KEYS = {
  all: ["offers"] as const,
  lists: () => [...OFFERS_KEYS.all, "list"] as const,
  list: (filters: Record<string, unknown>) =>
    [...OFFERS_KEYS.lists(), filters] as const,
  details: () => [...OFFERS_KEYS.all, "detail"] as const,
  detail: (id: number | string) => [...OFFERS_KEYS.details(), id] as const,
};

/**
 * Hook để lấy thông tin của một offer cụ thể
 */
export function useOffer(id: number | string) {
  return useQuery<ApiResponse<Offer>, Error>({
    queryKey: OFFERS_KEYS.detail(id),
    queryFn: () => getOffer(Number(id)),
    staleTime: 5 * 60 * 1000, // 5 phút
    retry: 1, // Số lần retry nếu failed
  });
}

/**
 * Hook để lấy tất cả các offers
 */
export function useOffers() {
  return useQuery<ApiResponse<Offer> | Offer[], Error>({
    queryKey: OFFERS_KEYS.lists(),
    queryFn: getOffers,
    staleTime: 5 * 60 * 1000, // 5 phút
  });
}

/**
 * Hook để lấy thông tin offer và xử lý cả trường hợp là direct data object
 * hoặc ApiResponse<Offer>
 */
export function useOfferData(id: number | string) {
  const { data, isLoading, error } = useOffer(id);

  // Xử lý data để luôn trả về Offer object
  const offer = data && (("data" in data ? data.data : data) as Offer);

  return {
    offer,
    isLoading,
    error,
  };
}
