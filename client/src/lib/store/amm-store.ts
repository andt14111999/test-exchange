import { create } from "zustand";

interface Pool {
  token0: string;
  token1: string;
  sqrtPriceX96: string;
  liquidity: string;
}

interface AMMState {
  pools: Record<string, Pool>;
  initializePools: () => void;
  swap: (params: {
    inputToken: string;
    outputToken: string;
    inputAmount: string;
  }) => Promise<void>;
}

export const useAMMStore = create<AMMState>((set) => ({
  pools: {},
  initializePools: () => {
    // Mock data for now
    const mockPools: Record<string, Pool> = {
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
    };

    set({ pools: mockPools });
  },
  swap: async ({ inputToken, outputToken, inputAmount }) => {
    // Implement swap logic here
    console.log("Swapping", inputAmount, inputToken, "to", outputToken);
  },
}));
