import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  getTrades,
  getTrade,
  createTrade,
  markTradePaid,
  releaseTrade,
  disputeTrade,
  cancelTrade,
  type ApiTrade,
  type TradeListParams,
  type CreateTradeParams,
  type DisputeTradeParams,
  type CancelTradeParams,
} from "@/lib/api/trades";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
  },
}));

describe("Trades API", () => {
  const mockTrade: ApiTrade = {
    id: "1",
    ref: "TRADE123",
    coin_currency: "USDT",
    fiat_currency: "USD",
    fiat_amount: "3000",
    coin_amount: "100",
    price: "30",
    status: "pending",
    taker_side: "buy",
    payment_method: "bank",
    payment_details: {
      bank_name: "Test Bank",
      bank_account_name: "John Doe",
      bank_account_number: "1234567890",
      bank_branch: "Main Branch",
    },
    amount_after_fee: "98",
    coin_trading_fee: "2",
    fee_ratio: "0.02",
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
    dispute_reason: undefined,
    dispute_resolution: undefined,
    buyer: {
      id: "1",
      email: "buyer@example.com",
      display_name: "buyer",
    },
    seller: {
      id: "2",
      email: "seller@example.com",
      display_name: "seller",
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getTrades", () => {
    it("should fetch all trades successfully without params", async () => {
      const mockResponse = { data: [mockTrade] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getTrades();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.trades.list, {
        params: undefined,
      });
      expect(result).toEqual(mockResponse.data);
    });

    it("should fetch trades with filter params", async () => {
      const params: TradeListParams = {
        status: "completed",
        role: "buyer",
        page: 1,
        per_page: 10,
      };
      const mockResponse = { data: [mockTrade] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getTrades(params);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.trades.list, {
        params,
      });
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching trades", async () => {
      const mockError = new Error("Failed to fetch trades");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getTrades()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("getTrade", () => {
    it("should fetch specific trade successfully", async () => {
      const mockResponse = { data: mockTrade };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getTrade("1");

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.trades.get("1"));
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching specific trade", async () => {
      const mockError = new Error("Failed to fetch trade");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getTrade("1")).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("createTrade", () => {
    it("should create trade successfully", async () => {
      const params: CreateTradeParams = {
        offer_id: "1",
        coin_amount: 100,
      };
      const mockResponse = { data: mockTrade };
      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await createTrade(params);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.trades.create,
        params,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when creating trade", async () => {
      const mockError = new Error("Failed to create trade");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      const params: CreateTradeParams = {
        offer_id: "1",
        coin_amount: 100,
      };

      await expect(createTrade(params)).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("markTradePaid", () => {
    it("should mark trade as paid successfully", async () => {
      const mockFile = new File(["test"], "test.jpg", { type: "image/jpeg" });
      const paymentDetails = {
        file: mockFile,
        description: "Payment receipt",
      };
      const mockResponse = {
        data: {
          ...mockTrade,
          has_payment_proof: true,
          payment_receipt_details: {
            proof_url: "https://example.com/proof.jpg",
            description: "Payment receipt",
            uploaded_at: "2024-03-20T00:00:00Z",
          },
        },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await markTradePaid("1", paymentDetails);

      const expectedFormData = new FormData();
      expectedFormData.append("payment_receipt_details[file]", mockFile);
      expectedFormData.append(
        "payment_receipt_details[description]",
        "Payment receipt",
      );

      expect(apiClient.put).toHaveBeenCalledWith(
        API_ENDPOINTS.trades.markPaid("1"),
        expect.any(FormData),
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when marking trade as paid", async () => {
      const mockError = new Error("Failed to mark trade as paid");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      const mockFile = new File(["test"], "test.jpg", { type: "image/jpeg" });
      const paymentDetails = {
        file: mockFile,
        description: "Payment receipt",
      };

      await expect(markTradePaid("1", paymentDetails)).rejects.toThrow(
        mockError,
      );
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("releaseTrade", () => {
    it("should release trade successfully", async () => {
      const mockResponse = {
        data: {
          ...mockTrade,
          status: "completed",
          released_at: "2024-03-20T00:00:00Z",
        },
      };
      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await releaseTrade("1");

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.trades.release("1"),
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when releasing trade", async () => {
      const mockError = new Error("Failed to release trade");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(releaseTrade("1")).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("disputeTrade", () => {
    it("should dispute trade successfully", async () => {
      const params: DisputeTradeParams = {
        dispute_reason: "Payment not received",
      };
      const mockResponse = {
        data: {
          ...mockTrade,
          status: "disputed",
          disputed_at: "2024-03-20T00:00:00Z",
          dispute_reason: "Payment not received",
        },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await disputeTrade("1", params);

      expect(apiClient.put).toHaveBeenCalledWith(
        API_ENDPOINTS.trades.dispute("1"),
        params,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when disputing trade", async () => {
      const mockError = new Error("Failed to dispute trade");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      const params: DisputeTradeParams = {
        dispute_reason: "Payment not received",
      };

      await expect(disputeTrade("1", params)).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("cancelTrade", () => {
    it("should cancel trade successfully", async () => {
      const params: CancelTradeParams = {
        cancel_reason: "Changed my mind",
      };
      const mockResponse = {
        data: {
          ...mockTrade,
          status: "cancelled",
          cancelled_at: "2024-03-20T00:00:00Z",
        },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await cancelTrade("1", params);

      expect(apiClient.put).toHaveBeenCalledWith(
        API_ENDPOINTS.trades.cancel("1"),
        params,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when cancelling trade", async () => {
      const mockError = new Error("Failed to cancel trade");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      const params: CancelTradeParams = {
        cancel_reason: "Changed my mind",
      };

      await expect(cancelTrade("1", params)).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });
});
