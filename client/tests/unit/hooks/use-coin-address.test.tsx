import { renderHook, waitFor } from "@testing-library/react";
import { useCoinAddress } from "@/hooks/use-coin-address";
import { fetchCoinAddress, generateCoinAddress } from "@/lib/api/coin-address";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act } from "react-dom/test-utils";
import React from "react";

// Mock the API functions
jest.mock("@/lib/api/coin-address", () => ({
  fetchCoinAddress: jest.fn(),
  generateCoinAddress: jest.fn(),
}));

const mockFetchCoinAddress = fetchCoinAddress as jest.Mock;
const mockGenerateCoinAddress = generateCoinAddress as jest.Mock;

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return Wrapper;
};

describe("useCoinAddress", () => {
  const defaultParams = {
    coinCurrency: "btc",
    layer: "bitcoin",
  };

  const mockAddress = {
    status: "success",
    data: {
      address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
      network: "bitcoin",
      coin_currency: "btc",
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("fetches coin address successfully", async () => {
    mockFetchCoinAddress.mockResolvedValueOnce(mockAddress);

    const { result } = renderHook(() => useCoinAddress(defaultParams), {
      wrapper: createWrapper(),
    });

    // Initial state
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();

    // Wait for the query to resolve
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockAddress);
    expect(mockFetchCoinAddress).toHaveBeenCalledWith("btc", "bitcoin");
  });

  it("handles fetch error correctly", async () => {
    const error = new Error("Failed to fetch address");
    mockFetchCoinAddress.mockRejectedValueOnce(error);

    const { result } = renderHook(() => useCoinAddress(defaultParams), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBeDefined();
    expect(result.current.data).toBeUndefined();
  });

  it("generates new address successfully", async () => {
    const newAddress = {
      ...mockAddress,
      data: {
        ...mockAddress.data,
        address: "bc1qnewaddress123456789",
      },
    };

    mockGenerateCoinAddress.mockResolvedValueOnce(newAddress);
    mockFetchCoinAddress.mockResolvedValueOnce(mockAddress);

    const { result } = renderHook(() => useCoinAddress(defaultParams), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // Generate new address
    await act(async () => {
      result.current.generateAddress();
    });

    expect(mockGenerateCoinAddress).toHaveBeenCalledWith("btc", "bitcoin");
    expect(result.current.isGenerating).toBe(false);
  });

  it("handles generate address error correctly", async () => {
    const error = new Error("Failed to generate address");
    mockGenerateCoinAddress.mockRejectedValueOnce(error);
    mockFetchCoinAddress.mockResolvedValueOnce(mockAddress);

    const { result } = renderHook(() => useCoinAddress(defaultParams), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // Attempt to generate new address
    await act(async () => {
      result.current.generateAddress();
    });

    expect(mockGenerateCoinAddress).toHaveBeenCalledWith("btc", "bitcoin");
    expect(result.current.isGenerating).toBe(false);
  });

  it("invalidates and refetches query after successful generation", async () => {
    const newAddress = {
      ...mockAddress,
      data: {
        ...mockAddress.data,
        address: "bc1qnewaddress123456789",
      },
    };

    mockFetchCoinAddress
      .mockResolvedValueOnce(mockAddress)
      .mockResolvedValueOnce(newAddress);
    mockGenerateCoinAddress.mockResolvedValueOnce(newAddress);

    const { result } = renderHook(() => useCoinAddress(defaultParams), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(mockAddress);
    });

    // Generate new address
    await act(async () => {
      result.current.generateAddress();
    });

    // Wait for the refetch to complete
    await waitFor(() => {
      expect(mockFetchCoinAddress).toHaveBeenCalledTimes(2);
      expect(result.current.data).toEqual(newAddress);
    });
  });
});
