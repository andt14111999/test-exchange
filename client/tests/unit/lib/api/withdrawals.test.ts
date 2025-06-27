import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  createWithdrawal,
  getWithdrawalById,
  checkReceiver,
  CreateWithdrawalRequest,
  WithdrawalResponse,
} from "@/lib/api/withdrawals";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    post: jest.fn(),
    get: jest.fn(),
  },
}));

const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;

describe("Withdrawals API", () => {
  const mockCreateWithdrawalRequest: CreateWithdrawalRequest = {
    coin_amount: 1.23456789,
    coin_currency: "BTC",
    coin_layer: "BTC",
    coin_address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
  };

  const mockWithdrawalResponse: WithdrawalResponse = {
    id: "withdrawal-123",
    coin_amount: 1.23456789,
    coin_fee: 0.0001,
    coin_currency: "BTC",
    coin_layer: "BTC",
    coin_address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
    status: "PENDING",
    created_at: "2024-03-20T12:00:00Z",
    tx_hash: "0x123456789",
    network_name: "Bitcoin",
    is_internal_transfer: false,
    receiver_username: undefined,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("createWithdrawal", () => {
    it("should create a withdrawal successfully", async () => {
      const mockResponse = {
        data: {
          data: mockWithdrawalResponse,
        },
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await createWithdrawal(mockCreateWithdrawalRequest);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.withdrawals.create,
        mockCreateWithdrawalRequest,
      );
      expect(result).toEqual(mockWithdrawalResponse);
    });

    it("should handle error when creating withdrawal fails", async () => {
      const mockError = new Error("Failed to create withdrawal");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(
        createWithdrawal(mockCreateWithdrawalRequest),
      ).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });

    it("should create an internal transfer successfully", async () => {
      const internalTransferRequest: CreateWithdrawalRequest = {
        coin_amount: 50,
        coin_currency: "USDT",
        coin_layer: "internal",
        coin_address: "testuser123",
      };

      const internalTransferResponse: WithdrawalResponse = {
        ...mockWithdrawalResponse,
        id: "transfer_456",
        coin_amount: 50,
        coin_fee: 0,
        coin_layer: "internal",
        coin_address: "testuser123",
      };

      (apiClient.post as jest.Mock).mockResolvedValue({
        data: { data: internalTransferResponse },
      });

      const result = await createWithdrawal(internalTransferRequest);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.withdrawals.create,
        internalTransferRequest,
      );
      expect(result).toEqual(internalTransferResponse);
    });

    it("should handle API errors gracefully", async () => {
      const apiError = new Error("Insufficient balance");
      (apiClient.post as jest.Mock).mockRejectedValue(apiError);

      await expect(
        createWithdrawal(mockCreateWithdrawalRequest),
      ).rejects.toThrow("Insufficient balance");

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.withdrawals.create,
        mockCreateWithdrawalRequest,
      );
    });

    it("should handle network errors", async () => {
      const networkError = new Error("Network timeout");
      (apiClient.post as jest.Mock).mockRejectedValue(networkError);

      await expect(
        createWithdrawal(mockCreateWithdrawalRequest),
      ).rejects.toThrow("Network timeout");
    });
  });

  describe("getWithdrawalById", () => {
    const withdrawalId = "withdrawal-123";

    it("should fetch withdrawal by ID successfully", async () => {
      const mockResponse = {
        data: {
          data: mockWithdrawalResponse,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getWithdrawalById(withdrawalId);

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.withdrawals.get(withdrawalId),
      );
      expect(result).toEqual(mockWithdrawalResponse);
    });

    it("should handle error when fetching withdrawal fails", async () => {
      const mockError = new Error("Failed to fetch withdrawal");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getWithdrawalById(withdrawalId)).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });

    it("should get internal transfer by id successfully", async () => {
      const internalTransferResponse: WithdrawalResponse = {
        ...mockWithdrawalResponse,
        id: "transfer_456",
        coin_layer: "internal",
        coin_address: "testuser123",
        coin_fee: 0,
        tx_hash: undefined,
        network_name: undefined,
      };

      (apiClient.get as jest.Mock).mockResolvedValue({
        data: { data: internalTransferResponse },
      });

      const result = await getWithdrawalById("transfer_456");

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.withdrawals.get("transfer_456"),
      );
      expect(result).toEqual(internalTransferResponse);
    });

    it("should handle not found errors", async () => {
      const notFoundError = new Error("Withdrawal not found");
      (apiClient.get as jest.Mock).mockRejectedValue(notFoundError);

      await expect(getWithdrawalById("invalid_id")).rejects.toThrow(
        "Withdrawal not found",
      );

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.withdrawals.get("invalid_id"),
      );
    });
  });

  describe("checkReceiver", () => {
    it("should return true for valid username", async () => {
      (apiClient.get as jest.Mock).mockResolvedValue({
        data: true,
      });

      const result = await checkReceiver("validuser123");

      expect(apiClient.get).toHaveBeenCalledWith(
        "/coin_withdrawals/check_receiver",
        {
          params: {
            receiver_username: "validuser123",
          },
        },
      );
      expect(result).toBe(true);
    });

    it("should return false for invalid username", async () => {
      (apiClient.get as jest.Mock).mockResolvedValue({
        data: false,
      });

      const result = await checkReceiver("invaliduser");

      expect(apiClient.get).toHaveBeenCalledWith(
        "/coin_withdrawals/check_receiver",
        {
          params: {
            receiver_username: "invaliduser",
          },
        },
      );
      expect(result).toBe(false);
    });

    it("should return false for non-existent username", async () => {
      (apiClient.get as jest.Mock).mockResolvedValue({
        data: false,
      });

      const result = await checkReceiver("nonexistentuser");

      expect(result).toBe(false);
    });

    it("should handle API errors gracefully", async () => {
      const apiError = new Error("Service unavailable");
      (apiClient.get as jest.Mock).mockRejectedValue(apiError);

      await expect(checkReceiver("testuser")).rejects.toThrow(
        "Service unavailable",
      );

      expect(apiClient.get).toHaveBeenCalledWith(
        "/coin_withdrawals/check_receiver",
        {
          params: {
            receiver_username: "testuser",
          },
        },
      );
    });

    it("should handle network timeout errors", async () => {
      const timeoutError = new Error("Request timeout");
      (apiClient.get as jest.Mock).mockRejectedValue(timeoutError);

      await expect(checkReceiver("testuser")).rejects.toThrow(
        "Request timeout",
      );
    });

    it("should handle malformed response", async () => {
      (apiClient.get as jest.Mock).mockResolvedValue({
        data: false,
      });

      const result = await checkReceiver("testuser");

      expect(result).toBe(false);
    });

    it("should handle different username formats", async () => {
      const usernames = [
        "user123",
        "test_user",
        "user-name",
        "a".repeat(30), // 30 characters
        "abc", // 3 characters
      ];

      for (const username of usernames) {
        (apiClient.get as jest.Mock).mockResolvedValue({
          data: true,
        });

        const result = await checkReceiver(username);

        expect(apiClient.get).toHaveBeenCalledWith(
          "/coin_withdrawals/check_receiver",
          {
            params: {
              receiver_username: username,
            },
          },
        );
        expect(result).toBe(true);
      }
    });

    it("should handle empty or whitespace usernames", async () => {
      const invalidUsernames = ["", " ", "  ", "\t", "\n"];

      for (const username of invalidUsernames) {
        (apiClient.get as jest.Mock).mockResolvedValue({
          data: false,
        });

        const result = await checkReceiver(username);

        expect(result).toBe(false);
      }
    });
  });
});
