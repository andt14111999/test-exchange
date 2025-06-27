import React from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useOffer, useOffers, useOfferData } from "@/lib/api/hooks/use-offers";
import { getOffer, getOffers } from "@/lib/api/merchant";
import type { Offer, ApiResponse } from "@/lib/api/merchant";

// Mock the merchant API module
jest.mock("@/lib/api/merchant");

const mockGetOffer = getOffer as jest.Mock;
const mockGetOffers = getOffers as jest.Mock;

// Sample mock data
const mockOffer: Offer = {
  id: 1,
  offer_type: "buy",
  coin_currency: "USDT",
  currency: "USD",
  price: "30000",
  total_amount: "1000",
  min_amount: "100",
  max_amount: "1000",
  payment_method_id: 1,
  payment_time: 30,
  payment_details: {
    bank_name: "Test Bank",
    bank_account_number: "1234567890",
    bank_account_name: "John Doe",
    bank_id: "1",
  },
  country_code: "US",
  is_active: true,
  status: "active",
  created_at: "2024-03-20T00:00:00Z",
  updated_at: "2024-03-20T00:00:00Z",
};

const mockOffers: Offer[] = [
  mockOffer,
  {
    ...mockOffer,
    id: 2,
    total_amount: "2000",
    min_amount: "200",
    max_amount: "2000",
  },
];

const mockApiResponse: ApiResponse<Offer> = {
  data: mockOffer,
  message: "Success",
  status: "200",
};

const mockOffersApiResponse: ApiResponse<Offer[]> = {
  data: mockOffers,
  message: "Success",
  status: "200",
};

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
        retryDelay: 1,
        // Set a short timeout for tests
        networkMode: "always",
      },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, queryClient] as const;
};

describe("Offer Hooks", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("useOffer", () => {
    it("should fetch a single offer successfully", async () => {
      mockGetOffer.mockResolvedValueOnce(mockApiResponse);

      const { result } = renderHook(() => useOffer(1), {
        wrapper: createWrapper()[0],
      });

      // Initial state
      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockApiResponse);
      expect(mockGetOffer).toHaveBeenCalledWith(1);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch offer");
      mockGetOffer.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useOffer(1), {
        wrapper: createWrapper()[0],
      });

      // Initial state should be loading
      expect(result.current.isLoading).toBe(true);
      expect(result.current.isError).toBe(false);

      // Wait for error state to be set
      await waitFor(
        () => {
          expect(result.current.isError).toBe(true);
        },
        { timeout: 2000 },
      );

      // Now verify final state
      expect(result.current.isLoading).toBe(false);
      // React Query wraps the error in its own error message
      expect(result.current.error).toBeDefined();
      expect(result.current.error instanceof Error).toBe(true);
    });

    it("should use cached data when available", async () => {
      const [Wrapper, queryClient] = createWrapper();

      // Prefetch data
      await queryClient.prefetchQuery({
        queryKey: ["offers", "detail", 1],
        queryFn: () => Promise.resolve(mockApiResponse),
      });

      // Reset the mock to verify it's not called again
      mockGetOffer.mockClear();

      const { result } = renderHook(() => useOffer(1), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(mockApiResponse);
      expect(mockGetOffer).not.toHaveBeenCalled();
    });
  });

  describe("useOffers", () => {
    it("should fetch all offers successfully", async () => {
      mockGetOffers.mockResolvedValueOnce(mockOffersApiResponse);

      const { result } = renderHook(() => useOffers(), {
        wrapper: createWrapper()[0],
      });

      // Initial state
      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockOffersApiResponse);
      expect(mockGetOffers).toHaveBeenCalled();
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch offers");
      mockGetOffers.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useOffers(), {
        wrapper: createWrapper()[0],
      });

      // Wait for the query to reject
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(error);
    });

    it("should use cached data when available", async () => {
      const [Wrapper, queryClient] = createWrapper();

      // Prefetch data
      await queryClient.prefetchQuery({
        queryKey: ["offers", "list"],
        queryFn: () => Promise.resolve(mockOffersApiResponse),
      });

      // Reset the mock to verify it's not called again
      mockGetOffers.mockClear();

      const { result } = renderHook(() => useOffers(), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(mockOffersApiResponse);
      expect(mockGetOffers).not.toHaveBeenCalled();
    });
  });

  describe("useOfferData", () => {
    it("should handle ApiResponse data format", async () => {
      mockGetOffer.mockResolvedValueOnce(mockApiResponse);

      const { result } = renderHook(() => useOfferData(1), {
        wrapper: createWrapper()[0],
      });

      // Initial state
      expect(result.current.isLoading).toBe(true);
      expect(result.current.offer).toBeUndefined();

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.offer).toEqual(mockOffer);
      expect(mockGetOffer).toHaveBeenCalledWith(1);
    });

    it("should handle direct Offer data format", async () => {
      mockGetOffer.mockResolvedValueOnce(mockOffer);

      const { result } = renderHook(() => useOfferData(1), {
        wrapper: createWrapper()[0],
      });

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.offer).toEqual(mockOffer);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch offer");
      mockGetOffer.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useOfferData(1), {
        wrapper: createWrapper()[0],
      });

      // Initial state should be loading
      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();

      // Wait for error state to be set
      await waitFor(
        () => {
          expect(result.current.error).toBeDefined();
          expect(result.current.error instanceof Error).toBe(true);
          expect(result.current.offer).toBeUndefined();
        },
        { timeout: 2000 },
      );
    });

    it("should handle null data", async () => {
      mockGetOffer.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useOfferData(1), {
        wrapper: createWrapper()[0],
      });

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.offer).toBeNull();
    });
  });
});
