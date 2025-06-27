import React from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  usePaymentMethods,
  usePaymentMethod,
} from "@/lib/api/hooks/use-payment-methods";
import {
  getPaymentMethods,
  getPaymentMethod,
  PaymentMethod,
  PaymentMethodResponse,
} from "@/lib/api/payment-methods";

// Mock the payment-methods API module
jest.mock("@/lib/api/payment-methods");

const mockGetPaymentMethod = getPaymentMethod as jest.Mock;
const mockGetPaymentMethods = getPaymentMethods as jest.Mock;

// Sample mock data
const mockPaymentMethod: PaymentMethod = {
  id: 1,
  name: "bank_transfer",
  display_name: "Bank Transfer",
  description: "Transfer money directly to our bank account",
  country_code: "US",
  enabled: true,
  icon_url: null,
  fields_required: {
    bank_account: true,
    swift_code: false,
  },
  created_at: "2024-03-20T00:00:00Z",
  updated_at: "2024-03-20T00:00:00Z",
};

const mockPaymentMethods: PaymentMethod[] = [
  mockPaymentMethod,
  {
    ...mockPaymentMethod,
    id: 2,
    name: "credit_card",
    display_name: "Credit Card",
    description: "Pay with your credit card",
  },
];

const mockPaymentMethodResponse: PaymentMethodResponse = {
  data: mockPaymentMethod,
};

const mockPaymentMethodsResponse: PaymentMethodResponse = {
  data: mockPaymentMethods,
};

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
        retryDelay: 1,
        networkMode: "always",
        staleTime: Infinity,
      },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, queryClient] as const;
};

describe("Payment Method Hooks", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("usePaymentMethods", () => {
    it("should fetch all payment methods successfully", async () => {
      mockGetPaymentMethods.mockResolvedValueOnce(mockPaymentMethodsResponse);

      const { result } = renderHook(() => usePaymentMethods(), {
        wrapper: createWrapper()[0],
      });

      // Initial state
      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockPaymentMethodsResponse);
      expect(mockGetPaymentMethods).toHaveBeenCalled();
    });

    it("should fetch payment methods with country code and enabled filters", async () => {
      mockGetPaymentMethods.mockResolvedValueOnce(mockPaymentMethodsResponse);

      const { result } = renderHook(() => usePaymentMethods("US", true), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockPaymentMethodsResponse);
      expect(mockGetPaymentMethods).toHaveBeenCalledWith("US", true);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch payment methods");
      mockGetPaymentMethods.mockRejectedValueOnce(error);

      const { result } = renderHook(() => usePaymentMethods(), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(error);
    });

    it("should use cached data when available", async () => {
      const [Wrapper, queryClient] = createWrapper();

      // Set the cached data directly
      queryClient.setQueryData(
        ["paymentMethods", { countryCode: undefined, enabled: undefined }],
        mockPaymentMethodsResponse,
      );

      const { result } = renderHook(() => usePaymentMethods(), {
        wrapper: Wrapper,
      });

      // Data should be available immediately without loading
      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(mockPaymentMethodsResponse);
      expect(mockGetPaymentMethods).not.toHaveBeenCalled();
    });
  });

  describe("usePaymentMethod", () => {
    it("should fetch a single payment method successfully", async () => {
      mockGetPaymentMethod.mockResolvedValueOnce(mockPaymentMethodResponse);

      const { result } = renderHook(() => usePaymentMethod(1), {
        wrapper: createWrapper()[0],
      });

      // Initial state
      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Wait for the query to resolve
      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data).toEqual(mockPaymentMethodResponse);
      expect(mockGetPaymentMethod).toHaveBeenCalledWith(1);
    });

    it("should not fetch when id is undefined", () => {
      const { result } = renderHook(() => usePaymentMethod(undefined), {
        wrapper: createWrapper()[0],
      });

      expect(result.current.isLoading).toBe(false);
      expect(mockGetPaymentMethod).not.toHaveBeenCalled();
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch payment method");
      mockGetPaymentMethod.mockRejectedValueOnce(error);

      const { result } = renderHook(() => usePaymentMethod(1), {
        wrapper: createWrapper()[0],
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(error);
    });

    it("should use cached data when available", async () => {
      const [Wrapper, queryClient] = createWrapper();

      // Set the cached data directly
      queryClient.setQueryData(["paymentMethod", 1], mockPaymentMethodResponse);

      const { result } = renderHook(() => usePaymentMethod(1), {
        wrapper: Wrapper,
      });

      // Data should be available immediately without loading
      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(mockPaymentMethodResponse);
      expect(mockGetPaymentMethod).not.toHaveBeenCalled();
    });
  });
});
