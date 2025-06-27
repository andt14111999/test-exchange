import { act, renderHook } from "@testing-library/react";
import { AmmPoolProvider, useAmmPools } from "@/providers/amm-pool-provider";
import { useAmmPoolChannel } from "@/hooks/use-amm-pool-channel";
import type { ApiPool } from "@/lib/api/pools";

// Mock the useAmmPoolChannel hook
jest.mock("@/hooks/use-amm-pool-channel", () => ({
  useAmmPoolChannel: jest.fn(),
}));

// Mock console.log to avoid noise in test output
const originalConsoleLog = console.log;
beforeAll(() => {
  console.log = jest.fn();
});

afterAll(() => {
  console.log = originalConsoleLog;
});

const mockPool: ApiPool = {
  id: 1,
  pair: "btc_usdt",
  token0: "btc",
  token1: "usdt",
  tick_spacing: 60,
  fee_percentage: "0.003",
  current_tick: 100,
  sqrt_price: "1000000000",
  price: "20000",
  apr: 5.5,
  tvl_in_token0: "10",
  tvl_in_token1: "200000",
  created_at: 1647734400,
  updated_at: 1647734400,
};

// Type for the mock implementation
type MockChannelCallback = (pool: ApiPool) => void;
interface MockChannelImpl {
  latestCallback?: MockChannelCallback;
}

const mockChannelImpl: MockChannelImpl = {};

describe("AmmPoolProvider", () => {
  beforeEach(() => {
    (useAmmPoolChannel as jest.Mock).mockImplementation(({ onPoolUpdate }) => {
      // Store the callback for manual triggering in tests
      mockChannelImpl.latestCallback = onPoolUpdate;
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
    mockChannelImpl.latestCallback = undefined;
  });

  it("should throw error when used outside provider", () => {
    // Wrap in a try-catch since renderHook will throw
    expect(() => {
      const { result } = renderHook(() => useAmmPools());
      // Access result.current to trigger the hook
      console.log(result.current);
    }).toThrow("useAmmPools must be used within an AmmPoolProvider");
  });

  it("should initialize with empty pools", () => {
    const { result } = renderHook(() => useAmmPools(), {
      wrapper: AmmPoolProvider,
    });

    expect(result.current.pools.size).toBe(0);
    expect(result.current.getAllPools()).toEqual([]);
    expect(result.current.getPool(1)).toBeUndefined();
  });

  it("should add new pool", () => {
    const { result } = renderHook(() => useAmmPools(), {
      wrapper: AmmPoolProvider,
    });

    act(() => {
      result.current.updatePool(mockPool);
    });

    expect(result.current.pools.size).toBe(1);
    expect(result.current.getPool(1)).toBeDefined();
    expect(result.current.getAllPools()).toHaveLength(1);
    expect(console.log).toHaveBeenCalledWith(
      expect.stringContaining("New pool added"),
    );
  });

  it("should update existing pool only when changes exist", () => {
    const { result } = renderHook(() => useAmmPools(), {
      wrapper: AmmPoolProvider,
    });

    // Add initial pool
    act(() => {
      result.current.updatePool(mockPool);
    });

    // Update with same data
    act(() => {
      result.current.updatePool(mockPool);
    });

    // Update with modified data
    act(() => {
      result.current.updatePool({
        ...mockPool,
        price: "25000",
      });
    });

    expect(result.current.pools.size).toBe(1);
    const updatedPool = result.current.getPool(1);
    expect(updatedPool?.price.toString()).toBe("25000");
    expect(console.log).toHaveBeenCalledWith(
      expect.stringContaining("Updated pool"),
    );
  });

  it("should handle pool updates through channel", () => {
    const { result } = renderHook(() => useAmmPools(), {
      wrapper: AmmPoolProvider,
    });

    // Simulate pool update through channel
    act(() => {
      if (mockChannelImpl.latestCallback) {
        mockChannelImpl.latestCallback(mockPool);
      }
    });

    expect(result.current.pools.size).toBe(1);
    expect(result.current.getPool(1)).toBeDefined();
    expect(result.current.getAllPools()).toHaveLength(1);
  });

  it("should maintain separate pool instances", () => {
    const { result } = renderHook(() => useAmmPools(), {
      wrapper: AmmPoolProvider,
    });

    const secondPool: ApiPool = {
      ...mockPool,
      id: 2,
      pair: "eth_usdt",
      token0: "eth",
      token1: "usdt",
    };

    act(() => {
      result.current.updatePool(mockPool);
      result.current.updatePool(secondPool);
    });

    expect(result.current.pools.size).toBe(2);
    expect(result.current.getAllPools()).toHaveLength(2);
    const pool1 = result.current.getPool(1);
    const pool2 = result.current.getPool(2);
    expect(pool1?.pair).toBe("btc_usdt");
    expect(pool2?.pair).toBe("eth_usdt");
  });
});
