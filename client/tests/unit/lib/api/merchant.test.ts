import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  registerAsMerchant,
  getEscrows,
  getEscrow,
  createEscrow,
  cancelEscrow,
  getOffers,
  getOffer,
  createOffer,
  updateOffer,
  deleteOffer,
  enableOffer,
  disableOffer,
  setOfferOnlineStatus,
  getMerchantOffers,
  type MerchantRegistrationResponse,
  type Escrow,
  type Offer,
  type CreateOfferRequest,
  type UpdateOfferRequest,
} from "@/lib/api/merchant";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    post: jest.fn(),
    get: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe("Merchant API", () => {
  const mockEscrow: Escrow = {
    id: 1,
    usdt_amount: "100",
    fiat_amount: "3000",
    fiat_currency: "USD",
    status: "pending",
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
  };

  const mockOffer: Offer = {
    id: 1,
    offer_type: "buy",
    coin_currency: "USDT",
    currency: "USD",
    price: "30000",
    total_amount: "1000",
    available_amount: "500",
    min_amount: "100",
    max_amount: "1000",
    payment_method_id: 1,
    payment_time: 30,
    payment_details: {
      bank_name: "Test Bank",
      bank_account_number: "1234567890",
      bank_account_name: "John Doe",
    },
    country_code: "US",
    is_active: true,
    online: true,
    status: "active",
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
    merchant_display_name: "TestMerchant",
    user_id: 1,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("registerAsMerchant", () => {
    it("should register user as merchant successfully", async () => {
      const mockResponse: { data: MerchantRegistrationResponse } = {
        data: {
          status: "success",
          data: {
            id: 1,
            email: "test@example.com",
            role: "merchant",
          },
        },
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await registerAsMerchant();

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.merchant.register,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error during merchant registration", async () => {
      const mockError = new Error("Registration failed");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(registerAsMerchant()).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("Escrow Operations", () => {
    describe("getEscrows", () => {
      it("should fetch all escrows successfully", async () => {
        const mockResponse = { data: [mockEscrow] };
        (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await getEscrows();

        expect(apiClient.get).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.escrows.list,
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when fetching escrows", async () => {
        const mockError = new Error("Failed to fetch escrows");
        (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(getEscrows()).rejects.toThrow(mockError);
        expect(apiClient.get).toHaveBeenCalledTimes(1);
      });
    });

    describe("getEscrow", () => {
      it("should fetch specific escrow successfully", async () => {
        const mockResponse = { data: mockEscrow };
        (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await getEscrow(1);

        expect(apiClient.get).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.escrows.get(1),
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when fetching specific escrow", async () => {
        const mockError = new Error("Failed to fetch escrow");
        (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(getEscrow(1)).rejects.toThrow(mockError);
        expect(apiClient.get).toHaveBeenCalledTimes(1);
      });
    });

    describe("createEscrow", () => {
      it("should create escrow successfully", async () => {
        const mockResponse = { data: mockEscrow };
        (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await createEscrow("100", "USD");

        expect(apiClient.post).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.escrows.create,
          {
            usdt_amount: "100",
            fiat_currency: "USD",
          },
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when creating escrow", async () => {
        const mockError = new Error("Failed to create escrow");
        (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(createEscrow("100", "USD")).rejects.toThrow(mockError);
        expect(apiClient.post).toHaveBeenCalledTimes(1);
      });
    });

    describe("cancelEscrow", () => {
      it("should cancel escrow successfully", async () => {
        const mockResponse = { data: { ...mockEscrow, status: "cancelled" } };
        (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await cancelEscrow(1);

        expect(apiClient.post).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.escrows.cancel(1),
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when cancelling escrow", async () => {
        const mockError = new Error("Failed to cancel escrow");
        (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(cancelEscrow(1)).rejects.toThrow(mockError);
        expect(apiClient.post).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe("Offer Operations", () => {
    describe("getOffers", () => {
      it("should fetch all offers successfully", async () => {
        const mockResponse = { data: [mockOffer] };
        (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await getOffers();

        expect(apiClient.get).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.list,
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when fetching offers", async () => {
        const mockError = new Error("Failed to fetch offers");
        (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(getOffers()).rejects.toThrow(mockError);
        expect(apiClient.get).toHaveBeenCalledTimes(1);
      });
    });

    describe("getOffer", () => {
      it("should fetch specific offer successfully", async () => {
        const mockResponse = { data: mockOffer };
        (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await getOffer(1);

        expect(apiClient.get).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.get(1),
        );
        expect(result).toEqual({ status: "success", data: mockOffer });
      });

      it("should handle direct offer response", async () => {
        (apiClient.get as jest.Mock).mockResolvedValueOnce({ data: mockOffer });

        const result = await getOffer(1);

        expect(result).toEqual({ status: "success", data: mockOffer });
      });

      it("should handle error when fetching specific offer", async () => {
        const mockError = new Error("Failed to fetch offer");
        (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

        const result = await getOffer(1);
        expect(result).toEqual({
          status: "error",
          data: {},
          message: "Failed to fetch offer",
        });
      });

      it("should handle invalid response", async () => {
        (apiClient.get as jest.Mock).mockResolvedValueOnce(null);

        const result = await getOffer(1);
        expect(result).toEqual({
          status: "error",
          data: {},
          message: "Failed to fetch offer data",
        });
      });
    });

    describe("createOffer", () => {
      const createOfferData: CreateOfferRequest = {
        offer_type: "buy",
        coin_currency: "USDT",
        currency: "USD",
        price: 30000,
        total_amount: 1000,
        min_amount: 100,
        max_amount: 1000,
        payment_method_id: 1,
        payment_time: 30,
        payment_details: {
          bank_name: "Test Bank",
          bank_account_number: "1234567890",
          bank_account_name: "John Doe",
        },
        country_code: "US",
      };

      it("should create offer successfully", async () => {
        const mockResponse = { data: mockOffer };
        (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await createOffer(createOfferData);

        expect(apiClient.post).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.create,
          createOfferData,
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when creating offer", async () => {
        const mockError = new Error("Failed to create offer");
        (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(createOffer(createOfferData)).rejects.toThrow(mockError);
        expect(apiClient.post).toHaveBeenCalledTimes(1);
      });
    });

    describe("updateOffer", () => {
      const updateOfferData: UpdateOfferRequest = {
        price: 31000,
        total_amount: 2000,
        is_active: true,
      };

      it("should update offer successfully", async () => {
        const mockResponse = { data: { ...mockOffer, ...updateOfferData } };
        (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await updateOffer(1, updateOfferData);

        expect(apiClient.put).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.update(1),
          updateOfferData,
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when updating offer", async () => {
        const mockError = new Error("Failed to update offer");
        (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(updateOffer(1, updateOfferData)).rejects.toThrow(
          mockError,
        );
        expect(apiClient.put).toHaveBeenCalledTimes(1);
      });
    });

    describe("deleteOffer", () => {
      it("should delete offer successfully", async () => {
        const mockResponse = { data: { ...mockOffer, deleted: true } };
        (apiClient.delete as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await deleteOffer(1);

        expect(apiClient.delete).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.delete(1),
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when deleting offer", async () => {
        const mockError = new Error("Failed to delete offer");
        (apiClient.delete as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(deleteOffer(1)).rejects.toThrow(mockError);
        expect(apiClient.delete).toHaveBeenCalledTimes(1);
      });
    });

    describe("enableOffer", () => {
      it("should enable offer successfully", async () => {
        const mockResponse = { data: { ...mockOffer, is_active: true } };
        (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await enableOffer(1);

        expect(apiClient.put).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.enable(1),
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when enabling offer", async () => {
        const mockError = new Error("Failed to enable offer");
        (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(enableOffer(1)).rejects.toThrow(mockError);
        expect(apiClient.put).toHaveBeenCalledTimes(1);
      });
    });

    describe("disableOffer", () => {
      it("should disable offer successfully", async () => {
        const mockResponse = { data: { ...mockOffer, is_active: false } };
        (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await disableOffer(1);

        expect(apiClient.put).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.disable(1),
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when disabling offer", async () => {
        const mockError = new Error("Failed to disable offer");
        (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(disableOffer(1)).rejects.toThrow(mockError);
        expect(apiClient.put).toHaveBeenCalledTimes(1);
      });
    });

    describe("setOfferOnlineStatus", () => {
      it("should set offer online status successfully", async () => {
        const mockResponse = { data: { ...mockOffer, online: true } };
        (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await setOfferOnlineStatus(1, true);

        expect(apiClient.put).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.setOnlineStatus(1),
          { online: true },
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when setting offer online status", async () => {
        const mockError = new Error("Failed to set offer online status");
        (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(setOfferOnlineStatus(1, true)).rejects.toThrow(mockError);
        expect(apiClient.put).toHaveBeenCalledTimes(1);
      });
    });

    describe("getMerchantOffers", () => {
      it("should fetch merchant offers successfully", async () => {
        const mockResponse = { data: [mockOffer] };
        (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

        const result = await getMerchantOffers();

        expect(apiClient.get).toHaveBeenCalledWith(
          API_ENDPOINTS.merchant.offers.merchantList,
        );
        expect(result).toEqual(mockResponse.data);
      });

      it("should handle error when fetching merchant offers", async () => {
        const mockError = new Error("Failed to fetch merchant offers");
        (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

        await expect(getMerchantOffers()).rejects.toThrow(mockError);
        expect(apiClient.get).toHaveBeenCalledTimes(1);
      });
    });
  });
});
