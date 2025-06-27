import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  getPaymentMethods,
  getPaymentMethod,
  type PaymentMethod,
  type PaymentMethodResponse,
} from "@/lib/api/payment-methods";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Payment Methods API", () => {
  const mockPaymentMethod: PaymentMethod = {
    id: 1,
    name: "bank_transfer",
    display_name: "Bank Transfer",
    description: "Transfer money directly to our bank account",
    country_code: "US",
    enabled: true,
    icon_url: "https://example.com/icon.png",
    fields_required: {
      bank_account: true,
      swift_code: false,
    },
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getPaymentMethods", () => {
    it("should fetch all payment methods successfully without params", async () => {
      const mockResponse = { data: [mockPaymentMethod] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getPaymentMethods();

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.paymentMethods.list,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should fetch payment methods with country code", async () => {
      const mockResponse = { data: [mockPaymentMethod] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getPaymentMethods("US");

      expect(apiClient.get).toHaveBeenCalledWith(
        `${API_ENDPOINTS.paymentMethods.list}?country_code=US`,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should fetch payment methods with enabled status", async () => {
      const mockResponse = { data: [mockPaymentMethod] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getPaymentMethods(undefined, true);

      expect(apiClient.get).toHaveBeenCalledWith(
        `${API_ENDPOINTS.paymentMethods.list}?enabled=true`,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should fetch payment methods with both country code and enabled status", async () => {
      const mockResponse = { data: [mockPaymentMethod] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getPaymentMethods("US", true);

      expect(apiClient.get).toHaveBeenCalledWith(
        `${API_ENDPOINTS.paymentMethods.list}?country_code=US&enabled=true`,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching payment methods", async () => {
      const mockError = new Error("Failed to fetch payment methods");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getPaymentMethods()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("getPaymentMethod", () => {
    it("should fetch specific payment method successfully", async () => {
      const mockResponse: { data: PaymentMethodResponse } = {
        data: {
          status: "success",
          data: mockPaymentMethod,
        },
      };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getPaymentMethod(1);

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.paymentMethods.get(1),
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching specific payment method", async () => {
      const mockError = new Error("Failed to fetch payment method");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getPaymentMethod(1)).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
