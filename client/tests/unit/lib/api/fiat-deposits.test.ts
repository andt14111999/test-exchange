import { apiClient } from "@/lib/api/client";
import {
  createTradeRelatedDeposit,
  getFiatDeposit,
  getTradeFiatDeposit,
  markMoneySent,
  markTradeDepositMoneySent,
  verifyOwnership,
  verifyTradeDepositOwnership,
  cancelDeposit,
  cancelTradeDeposit,
  getFiatDeposits,
  getFiatWithdrawals,
  type FiatWithdrawal,
} from "@/lib/api/fiat-deposits";
import type {
  FiatDeposit,
  CreateFiatDepositParams,
  OwnershipVerificationParams,
} from "@/types/fiat-deposits";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
  },
}));

describe("Fiat Deposits API", () => {
  const mockFiatDeposit: FiatDeposit = {
    id: "123",
    currency: "USD",
    country_code: "US",
    fiat_amount: 1000,
    deposit_fee: 10,
    amount_after_fee: 990,
    status: "pending",
    created_at: "2024-03-20T00:00:00Z",
    user_id: "user-123",
    fiat_account_id: "account-123",
    requires_ownership_verification: false,
    payable_id: "trade-123",
    payable_type: "Trade",
  };

  const mockFiatWithdrawal: FiatWithdrawal = {
    id: "456",
    fiat_amount: 500,
    currency: "USD",
    status: "pending",
    created_at: "2024-03-20T00:00:00Z",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("createTradeRelatedDeposit", () => {
    const createParams: CreateFiatDepositParams = {
      currency: "USD",
      country_code: "US",
      fiat_amount: 1000,
      fiat_account_id: "account-123",
      memo: "Test deposit",
    };

    it("should create a trade-related deposit successfully", async () => {
      const mockResponse = { data: mockFiatDeposit };
      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await createTradeRelatedDeposit("trade-123", createParams);

      expect(apiClient.post).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit",
        createParams,
      );
      expect(result).toEqual(mockFiatDeposit);
    });

    it("should handle error when creating a trade-related deposit fails", async () => {
      const mockError = new Error("Failed to create deposit");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        createTradeRelatedDeposit("trade-123", createParams),
      ).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("getFiatDeposit", () => {
    it("should fetch a single fiat deposit successfully", async () => {
      const mockResponse = { data: mockFiatDeposit };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getFiatDeposit("123");

      expect(apiClient.get).toHaveBeenCalledWith("/fiat_deposits/123");
      expect(result).toEqual(mockFiatDeposit);
    });

    it("should handle error when fetching a single fiat deposit fails", async () => {
      const mockError = new Error("Failed to fetch deposit");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getFiatDeposit("123")).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("getTradeFiatDeposit", () => {
    it("should fetch a trade's fiat deposit successfully", async () => {
      const mockResponse = { data: mockFiatDeposit };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getTradeFiatDeposit("trade-123");

      expect(apiClient.get).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit",
      );
      expect(result).toEqual(mockFiatDeposit);
    });

    it("should handle error when fetching a trade's fiat deposit fails", async () => {
      const mockError = new Error("Failed to fetch trade deposit");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getTradeFiatDeposit("trade-123")).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("markMoneySent", () => {
    const paymentProof = {
      payment_proof_url: "https://example.com/proof.jpg",
      payment_description: "Bank transfer receipt",
      mark_as_sent: true,
      additional_proof: false,
    };

    it("should mark money as sent successfully with payment proof", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "money_sent" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await markMoneySent("123", paymentProof);

      expect(apiClient.put).toHaveBeenCalledWith(
        "/fiat_deposits/123/money_sent",
        paymentProof,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should mark money as sent successfully without payment proof", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "money_sent" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await markMoneySent("123");

      expect(apiClient.put).toHaveBeenCalledWith(
        "/fiat_deposits/123/money_sent",
        undefined,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when marking money as sent fails", async () => {
      const mockError = new Error("Failed to mark money as sent");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(markMoneySent("123", paymentProof)).rejects.toThrow(
        mockError,
      );
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("markTradeDepositMoneySent", () => {
    const paymentProof = {
      payment_proof_url: "https://example.com/proof.jpg",
      payment_description: "Bank transfer receipt",
      mark_as_sent: true,
      additional_proof: false,
    };

    it("should mark trade deposit money as sent successfully with payment proof", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "money_sent" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await markTradeDepositMoneySent("trade-123", paymentProof);

      expect(apiClient.put).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit/money_sent",
        paymentProof,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should mark trade deposit money as sent successfully without payment proof", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "money_sent" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await markTradeDepositMoneySent("trade-123");

      expect(apiClient.put).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit/money_sent",
        undefined,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when marking trade deposit money as sent fails", async () => {
      const mockError = new Error("Failed to mark trade deposit money as sent");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        markTradeDepositMoneySent("trade-123", paymentProof),
      ).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("verifyOwnership", () => {
    const verificationParams: OwnershipVerificationParams = {
      sender_name: "John Doe",
      sender_account_number: "1234567890",
    };

    it("should verify ownership successfully", async () => {
      const mockResponse = { data: { ...mockFiatDeposit, status: "verified" } };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await verifyOwnership("123", verificationParams);

      expect(apiClient.put).toHaveBeenCalledWith(
        "/fiat_deposits/123/verify_ownership",
        verificationParams,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when verifying ownership fails", async () => {
      const mockError = new Error("Failed to verify ownership");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(verifyOwnership("123", verificationParams)).rejects.toThrow(
        mockError,
      );
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("verifyTradeDepositOwnership", () => {
    const verificationParams: OwnershipVerificationParams = {
      sender_name: "John Doe",
      sender_account_number: "1234567890",
    };

    it("should verify trade deposit ownership successfully", async () => {
      const mockResponse = { data: { ...mockFiatDeposit, status: "verified" } };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await verifyTradeDepositOwnership(
        "trade-123",
        verificationParams,
      );

      expect(apiClient.put).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit/verify_ownership",
        verificationParams,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when verifying trade deposit ownership fails", async () => {
      const mockError = new Error("Failed to verify trade deposit ownership");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        verifyTradeDepositOwnership("trade-123", verificationParams),
      ).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("cancelDeposit", () => {
    it("should cancel deposit successfully with reason", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "cancelled" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await cancelDeposit("123", "User requested cancellation");

      expect(apiClient.put).toHaveBeenCalledWith("/fiat_deposits/123/cancel", {
        cancel_reason: "User requested cancellation",
      });
      expect(result).toEqual(mockResponse.data);
    });

    it("should cancel deposit successfully without reason", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "cancelled" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await cancelDeposit("123");

      expect(apiClient.put).toHaveBeenCalledWith(
        "/fiat_deposits/123/cancel",
        {},
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when cancelling deposit fails", async () => {
      const mockError = new Error("Failed to cancel deposit");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(cancelDeposit("123")).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("cancelTradeDeposit", () => {
    it("should cancel trade deposit successfully with reason", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "cancelled" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await cancelTradeDeposit(
        "trade-123",
        "User requested cancellation",
      );

      expect(apiClient.put).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit/cancel",
        { cancel_reason: "User requested cancellation" },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should cancel trade deposit successfully without reason", async () => {
      const mockResponse = {
        data: { ...mockFiatDeposit, status: "cancelled" },
      };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await cancelTradeDeposit("trade-123");

      expect(apiClient.put).toHaveBeenCalledWith(
        "/trades/trade-123/fiat_deposit/cancel",
        {},
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when cancelling trade deposit fails", async () => {
      const mockError = new Error("Failed to cancel trade deposit");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(cancelTradeDeposit("trade-123")).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("getFiatDeposits", () => {
    it("should fetch fiat deposits without params successfully", async () => {
      const mockResponse = {
        data: {
          data: [mockFiatDeposit],
          meta: { total: 1, page: 1, per_page: 10 },
        },
      };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getFiatDeposits();

      expect(apiClient.get).toHaveBeenCalledWith("/fiat_deposits", {
        params: undefined,
      });
      expect(result).toEqual(mockResponse.data);
    });

    it("should fetch fiat deposits with params successfully", async () => {
      const params = {
        status: "pending",
        currency: "USD",
        page: 1,
        per_page: 10,
      };
      const mockResponse = {
        data: {
          data: [mockFiatDeposit],
          meta: { total: 1, page: 1, per_page: 10 },
        },
      };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getFiatDeposits(params);

      expect(apiClient.get).toHaveBeenCalledWith("/fiat_deposits", { params });
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching fiat deposits fails", async () => {
      const mockError = new Error("Failed to fetch fiat deposits");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getFiatDeposits()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("getFiatWithdrawals", () => {
    it("should fetch fiat withdrawals successfully", async () => {
      const mockResponse = { data: { data: [mockFiatWithdrawal] } };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getFiatWithdrawals();

      expect(apiClient.get).toHaveBeenCalledWith("/fiat_withdrawals", {
        params: undefined,
      });
      expect(result).toEqual({ data: [mockFiatWithdrawal] });
    });

    it("should handle error when fetching fiat withdrawals fails", async () => {
      const mockError = new Error("Failed to fetch fiat withdrawals");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getFiatWithdrawals()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
