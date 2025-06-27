import { apiClient } from "@/lib/api/client";
import { fetchTicks, Tick, TicksResponse } from "@/lib/api/amm-ticks";
import { API_ENDPOINTS } from "@/lib/api/config";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("AMM Ticks API", () => {
  const mockPoolPair = "BTC-USDT";

  const mockTicks: Tick[] = [
    {
      tick_index: 100,
      liquidity_gross: "1000",
      liquidity_net: "500",
      fee_growth_outside0: "0.1",
      fee_growth_outside1: "0.2",
      initialized: true,
    },
    {
      tick_index: 101,
      liquidity_gross: "2000",
      liquidity_net: "1000",
      fee_growth_outside0: "0.3",
      fee_growth_outside1: "0.4",
      initialized: true,
    },
  ];

  const mockTicksResponse: TicksResponse = {
    [mockPoolPair]: {
      ticks: mockTicks,
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchTicks", () => {
    it("should fetch ticks successfully", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockTicksResponse,
      });

      const result = await fetchTicks(mockPoolPair);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.ticks, {
        params: { pool_pair: mockPoolPair },
      });
      expect(result).toEqual(mockTicks);
    });

    it("should return empty array when response doesn't include pool pair", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: {},
      });

      const result = await fetchTicks(mockPoolPair);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.ticks, {
        params: { pool_pair: mockPoolPair },
      });
      expect(result).toEqual([]);
    });

    it("should return empty array when response includes pool pair but no ticks", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: { [mockPoolPair]: {} },
      });

      const result = await fetchTicks(mockPoolPair);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.ticks, {
        params: { pool_pair: mockPoolPair },
      });
      expect(result).toEqual([]);
    });

    it("should return empty array when response is null", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: null,
      });

      const result = await fetchTicks(mockPoolPair);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.ticks, {
        params: { pool_pair: mockPoolPair },
      });
      expect(result).toEqual([]);
    });

    it("should handle errors and return empty array", async () => {
      const mockError = new Error("Failed to fetch ticks");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      const consoleSpy = jest.spyOn(console, "error");

      const result = await fetchTicks(mockPoolPair);

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.amm.ticks, {
        params: { pool_pair: mockPoolPair },
      });
      expect(consoleSpy).toHaveBeenCalledWith(
        `Error fetching ticks for pool ${mockPoolPair}:`,
        mockError,
      );
      expect(result).toEqual([]);
    });
  });
});
