import { apiClient } from "@/lib/api/client";
import {
  executeSwap,
  fetchSwapOrders,
  fetchSwapOrderDetail,
  SwapOrder,
  SwapOrdersResponse,
} from "@/lib/api/amm-orders";
import { API_ENDPOINTS } from "@/lib/api/config";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    post: jest.fn(),
    get: jest.fn(),
  },
}));

describe("AMM Orders API", () => {
  const mockSwapOrder: SwapOrder = {
    id: 1,
    identifier: "test-id",
    zero_for_one: true,
    status: "completed",
    error_message: null,
    before_tick_index: 100,
    after_tick_index: 101,
    amount_specified: "1000",
    amount_estimated: "990",
    amount_actual: "995",
    amount_received: "995",
    slippage: "0.5",
    fees: { trading: 0.1 },
    created_at: 1234567890,
    updated_at: 1234567890,
  };

  const mockSwapOrdersResponse: SwapOrdersResponse = {
    amm_orders: [mockSwapOrder],
    meta: {
      current_page: 1,
      next_page: null,
      total_pages: 1,
      per_page: 10,
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("executeSwap", () => {
    const mockSwapParams = {
      poolPair: "BTC-USDT",
      zeroForOne: true,
      amountSpecified: "1000",
      amountEstimated: "990",
      slippage: 0.5,
    };

    it("should execute swap successfully", async () => {
      (apiClient.post as jest.Mock).mockResolvedValueOnce({
        data: mockSwapOrder,
      });

      const result = await executeSwap(mockSwapParams);

      expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.amm.orders, {
        pool_pair: mockSwapParams.poolPair,
        zero_for_one: mockSwapParams.zeroForOne,
        amount_specified: mockSwapParams.amountSpecified,
        amount_estimated: mockSwapParams.amountEstimated,
        slippage: mockSwapParams.slippage,
      });
      expect(result).toEqual(mockSwapOrder);
    });

    it("should handle error during swap execution", async () => {
      const mockError = new Error("Swap failed");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(executeSwap(mockSwapParams)).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("fetchSwapOrders", () => {
    it("should fetch swap orders successfully", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockSwapOrdersResponse,
      });

      const result = await fetchSwapOrders(1, 10);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.orders, {
        params: {
          page: 1,
          per_page: 10,
        },
      });
      expect(result).toEqual(mockSwapOrdersResponse);
    });

    it("should include status in params when provided", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockSwapOrdersResponse,
      });

      await fetchSwapOrders(1, 10, "completed");

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.orders, {
        params: {
          page: 1,
          per_page: 10,
          status: "completed",
        },
      });
    });

    it("should not include status in params when 'all'", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockSwapOrdersResponse,
      });

      await fetchSwapOrders(1, 10, "all");

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.orders, {
        params: {
          page: 1,
          per_page: 10,
        },
      });
    });

    it("should return empty response when data is missing", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: null,
      });

      const result = await fetchSwapOrders();

      expect(result).toEqual({
        amm_orders: [],
        meta: {
          current_page: 1,
          next_page: null,
          total_pages: 1,
          per_page: 10,
        },
      });
    });

    it("should handle error and return empty response", async () => {
      (apiClient.get as jest.Mock).mockRejectedValueOnce(
        new Error("Failed to fetch"),
      );

      const result = await fetchSwapOrders();

      expect(result).toEqual({
        amm_orders: [],
        meta: {
          current_page: 1,
          next_page: null,
          total_pages: 1,
          per_page: 10,
        },
      });
    });
  });

  describe("fetchSwapOrderDetail", () => {
    it("should fetch swap order detail successfully", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockSwapOrder,
      });

      const result = await fetchSwapOrderDetail(1);

      expect(apiClient.get).toHaveBeenCalledWith(
        `${API_ENDPOINTS.amm.orders}/1`,
      );
      expect(result).toEqual(mockSwapOrder);
    });

    it("should handle error during detail fetch", async () => {
      const mockError = new Error("Failed to fetch detail");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(fetchSwapOrderDetail(1)).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
