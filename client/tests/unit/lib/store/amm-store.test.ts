import { useAMMStore } from "@/lib/store/amm-store";

describe("AMM Store", () => {
  beforeEach(() => {
    // Reset store to initial state before each test
    useAMMStore.setState({
      pools: {},
      initializePools: useAMMStore.getState().initializePools,
      swap: useAMMStore.getState().swap,
    });
    jest.clearAllMocks();
  });

  describe("Initial State", () => {
    it("should have empty pools object", () => {
      expect(useAMMStore.getState().pools).toEqual({});
    });
  });

  describe("initializePools", () => {
    it("should initialize pools with mock data", () => {
      // Call initializePools
      useAMMStore.getState().initializePools();

      // Get the current state
      const state = useAMMStore.getState();

      // Check if pools are initialized correctly
      expect(state.pools).toEqual({
        "USDT-VND": {
          token0: "USDT",
          token1: "VND",
          sqrtPriceX96: "79228162514264337593543950336", // 1 USDT = 24,000 VND
          liquidity: "1000000000000000000",
        },
        "USDT-PHP": {
          token0: "USDT",
          token1: "PHP",
          sqrtPriceX96: "79228162514264337593543950336", // 1 USDT = 56 PHP
          liquidity: "1000000000000000000",
        },
        "USDT-NGN": {
          token0: "USDT",
          token1: "NGN",
          sqrtPriceX96: "79228162514264337593543950336", // 1 USDT = 1,500 NGN
          liquidity: "1000000000000000000",
        },
      });
    });

    it("should not affect other state properties when initializing pools", () => {
      // Add some custom state
      useAMMStore.setState({
        pools: {},
        initializePools: useAMMStore.getState().initializePools,
        swap: useAMMStore.getState().swap,
      });

      // Call initializePools
      useAMMStore.getState().initializePools();

      // Verify other state properties remain unchanged
      expect(typeof useAMMStore.getState().swap).toBe("function");
      expect(typeof useAMMStore.getState().initializePools).toBe("function");
    });
  });

  describe("swap", () => {
    beforeEach(() => {
      // Initialize pools before testing swap
      useAMMStore.getState().initializePools();
      // Spy on console.log
      jest.spyOn(console, "log");
    });

    it("should log swap parameters correctly", async () => {
      const swapParams = {
        inputToken: "USDT",
        outputToken: "VND",
        inputAmount: "100",
      };

      await useAMMStore.getState().swap(swapParams);

      expect(console.log).toHaveBeenCalledWith(
        "Swapping",
        swapParams.inputAmount,
        swapParams.inputToken,
        "to",
        swapParams.outputToken,
      );
    });

    it("should handle swap with different tokens", async () => {
      const swapParams = {
        inputToken: "USDT",
        outputToken: "PHP",
        inputAmount: "50",
      };

      await useAMMStore.getState().swap(swapParams);

      expect(console.log).toHaveBeenCalledWith(
        "Swapping",
        swapParams.inputAmount,
        swapParams.inputToken,
        "to",
        swapParams.outputToken,
      );
    });

    it("should not throw error for non-existent token pairs", async () => {
      const swapParams = {
        inputToken: "BTC",
        outputToken: "ETH",
        inputAmount: "1",
      };

      await expect(
        useAMMStore.getState().swap(swapParams),
      ).resolves.not.toThrow();
    });
  });
});
