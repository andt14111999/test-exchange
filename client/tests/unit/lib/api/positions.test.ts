import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  fetchPositions,
  fetchPositionById,
  createPosition,
  collectFee,
  closePosition,
  type AmmPosition,
  type AmmPositionsResponse,
  type AmmPositionResponse,
  type CreatePositionParams,
} from "@/lib/api/positions";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

describe("Positions API", () => {
  const mockPosition: AmmPosition = {
    id: 1,
    identifier: "pos_123",
    pool_pair: "btc_usdt",
    tick_lower_index: -100,
    tick_upper_index: 100,
    status: "open",
    error_message: null,
    liquidity: "1000000",
    amount0: "1",
    amount1: "20000",
    amount0_initial: "1",
    amount1_initial: "20000",
    slippage: "0.005",
    fee_growth_inside0_last: "100",
    fee_growth_inside1_last: "200",
    tokens_owed0: "0.1",
    tokens_owed1: "2000",
    fee_collected0: "0.05",
    fee_collected1: "1000",
    created_at: 1647734400,
    updated_at: 1647734400,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchPositions", () => {
    it("should fetch positions successfully with default parameters", async () => {
      const mockResponse: AmmPositionsResponse = {
        amm_positions: [mockPosition],
        meta: {
          current_page: 1,
          next_page: null,
          total_pages: 1,
          per_page: 10,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await fetchPositions();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.positions, {
        params: {
          status: "open",
          page: 1,
          per_page: 10,
        },
      });
      expect(result).toEqual(mockResponse);
    });

    it("should fetch positions with custom parameters", async () => {
      const mockResponse: AmmPositionsResponse = {
        amm_positions: [mockPosition],
        meta: {
          current_page: 2,
          next_page: 3,
          total_pages: 5,
          per_page: 20,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await fetchPositions("closed", 2, 20);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.positions, {
        params: {
          status: "closed",
          page: 2,
          per_page: 20,
        },
      });
      expect(result).toEqual(mockResponse);
    });

    it("should handle errors when fetching positions", async () => {
      const mockError = new Error("Failed to fetch positions");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      await expect(fetchPositions()).rejects.toThrow(mockError);
      expect(consoleSpy).toHaveBeenCalledWith(
        "Error fetching positions:",
        mockError,
      );
    });
  });

  describe("fetchPositionById", () => {
    it("should fetch position by id successfully", async () => {
      const mockResponse: AmmPositionResponse = {
        amm_position: mockPosition,
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await fetchPositionById(1);

      expect(apiClient.get).toHaveBeenCalledWith(
        API_ENDPOINTS.amm.positionDetail(1),
      );
      expect(result).toEqual(mockResponse);
    });

    it("should handle errors when fetching position by id", async () => {
      const mockError = new Error("Failed to fetch position");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      await expect(fetchPositionById(1)).rejects.toThrow(mockError);
      expect(consoleSpy).toHaveBeenCalledWith(
        "Error fetching position with id 1:",
        mockError,
      );
    });
  });

  describe("createPosition", () => {
    const createParams: CreatePositionParams = {
      pool_pair: "btc_usdt",
      tick_lower_index: -100,
      tick_upper_index: 100,
      amount0_initial: "1",
      amount1_initial: "20000",
      slippage: 0.005,
    };

    it("should create position successfully", async () => {
      const mockResponse: AmmPositionResponse = {
        amm_position: mockPosition,
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await createPosition(createParams);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.amm.positions,
        createParams,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should handle errors when creating position", async () => {
      const mockError = new Error("Failed to create position");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      await expect(createPosition(createParams)).rejects.toThrow(mockError);
      expect(consoleSpy).toHaveBeenCalledWith(
        "Error creating position:",
        mockError,
      );
    });
  });

  describe("collectFee", () => {
    it("should collect fee successfully", async () => {
      const mockResponse = { success: true };
      (apiClient.post as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await collectFee(1);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.amm.collectFee(1),
      );
      expect(result).toEqual(mockResponse);
    });

    it("should handle errors when collecting fee", async () => {
      const mockError = new Error("Failed to collect fee");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      await expect(collectFee(1)).rejects.toThrow(mockError);
      expect(consoleSpy).toHaveBeenCalledWith(
        "Error collecting fee for position 1:",
        mockError,
      );
    });
  });

  describe("closePosition", () => {
    it("should close position successfully", async () => {
      const mockResponse = { success: true };
      (apiClient.post as jest.Mock).mockResolvedValueOnce({
        data: mockResponse,
      });

      const result = await closePosition(1);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.amm.closePosition(1),
      );
      expect(result).toEqual(mockResponse);
    });

    it("should handle errors when closing position", async () => {
      const mockError = new Error("Failed to close position");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");
      await expect(closePosition(1)).rejects.toThrow(mockError);
      expect(consoleSpy).toHaveBeenCalledWith(
        "Error closing position 1:",
        mockError,
      );
    });
  });
});
