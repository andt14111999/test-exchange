import { apiClient } from "@/lib/api/client";
import {
  getExchangeRates,
  getWithdrawalFees,
  type ExchangeRates,
  type WithdrawalFees,
} from "@/lib/api/settings";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Settings API", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getExchangeRates", () => {
    it("should fetch exchange rates successfully", async () => {
      const mockExchangeRates: ExchangeRates = {
        usdt_to_vnd: 24500,
        usdt_to_php: 56.5,
        usdt_to_ngn: 1550,
      };

      const mockResponse = {
        data: {
          exchange_rates: mockExchangeRates,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getExchangeRates();

      expect(apiClient.get).toHaveBeenCalledWith("/settings/exchange_rates");
      expect(result).toEqual(mockExchangeRates);
    });

    it("should throw error when API call fails", async () => {
      const mockError = new Error("Failed to fetch exchange rates");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getExchangeRates()).rejects.toThrow(mockError);
    });
  });

  describe("getWithdrawalFees", () => {
    it("should fetch withdrawal fees successfully", async () => {
      const mockWithdrawalFees: WithdrawalFees = {
        usdt_erc20: 25,
        usdt_bep20: 1,
        usdt_trc20: 1,
        usdt_solana: 1,
      };

      const mockResponse = {
        data: {
          withdrawal_fees: mockWithdrawalFees,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getWithdrawalFees();

      expect(apiClient.get).toHaveBeenCalledWith("/settings/withdrawal_fees");
      expect(result).toEqual(mockWithdrawalFees);
    });

    it("should throw error when API call fails", async () => {
      const mockError = new Error("Failed to fetch withdrawal fees");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getWithdrawalFees()).rejects.toThrow(mockError);
    });

    it("should handle additional fee types", async () => {
      const mockWithdrawalFees: WithdrawalFees = {
        usdt_erc20: 25,
        usdt_bep20: 1,
        usdt_trc20: 1,
        usdt_solana: 1,
        new_network_fee: 2, // Testing the [key: string]: number index signature
      };

      const mockResponse = {
        data: {
          withdrawal_fees: mockWithdrawalFees,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getWithdrawalFees();

      expect(result).toEqual(mockWithdrawalFees);
      expect(result.new_network_fee).toBe(2);
    });
  });
});
