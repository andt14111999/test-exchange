import {
  estimateSwapV3,
  estimateSwap0to1,
  estimateSwap1to0,
} from "@/lib/amm/amm-math";
import { Tick } from "@/lib/api/amm-ticks";

// Mock console methods to avoid noise in test output
global.console.log = jest.fn();
global.console.error = jest.fn();

// Define an interface to match the return type that includes the error property
interface SwapResult {
  amountOut: number;
  amountIn: number;
  fee: number;
  priceAfter: number;
  priceImpact: number;
  originalPrice: number;
  error?: string;
}

describe("AMM Math Functions", () => {
  // Common mock data
  const mockPoolData = {
    sqrt_price: "1.5",
    fee_percentage: "0.003", // 0.3%
    tick_spacing: 1,
    tvl_in_token0: "1000000",
    tvl_in_token1: "1500000",
    price: "2.25", // 1.5 * 1.5
  };

  const mockPoolDataWithTickIndex = {
    ...mockPoolData,
    current_tick_index: 8100, // log(2.25) / log(1.0001) ≈ 8100
  };

  // Real world USDT-VND pool example
  const mockUsdtVndPoolData = {
    sqrt_price: "160.8534",
    fee_percentage: "0.003", // 0.3%
    tick_spacing: 1,
    tvl_in_token0: "1000000", // USDT
    tvl_in_token1: "23873950000", // VND (23,873.95 * 1,000,000)
    price: "25873.950228566875", // 160.8534^2
    current_tick_index: 101615,
  };

  const mockTicks: Tick[] = [
    {
      tick_index: 8000,
      liquidity_net: "1000",
      liquidity_gross: "1000",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 8050,
      liquidity_net: "2000",
      liquidity_gross: "2000",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 8100,
      liquidity_net: "3000",
      liquidity_gross: "3000",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 8150,
      liquidity_net: "4000",
      liquidity_gross: "4000",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 8200,
      liquidity_net: "5000",
      liquidity_gross: "5000",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
  ];

  // Add USDT-VND pool ticks
  const mockUsdtVndTicks: Tick[] = [
    {
      tick_index: 101500,
      liquidity_net: "34586991.05242795",
      liquidity_gross: "34586991.05242795",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 101550,
      liquidity_net: "15782343.87281943",
      liquidity_gross: "15782343.87281943",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 101600,
      liquidity_net: "22948722.16534187",
      liquidity_gross: "22948722.16534187",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 101615,
      liquidity_net: "10593847.26149873",
      liquidity_gross: "10593847.26149873",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 101650,
      liquidity_net: "28375869.45218764",
      liquidity_gross: "28375869.45218764",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
    {
      tick_index: 101700,
      liquidity_net: "16583927.12987654",
      liquidity_gross: "16583927.12987654",
      fee_growth_outside0: "0",
      fee_growth_outside1: "0",
      initialized: true,
    },
  ];

  describe("estimateSwap0to1", () => {
    it("should correctly estimate swap from token0 to token1", () => {
      const result = estimateSwap0to1(mockPoolData, 1000, []);

      // Check structure
      expect(result).toHaveProperty("amountIn");
      expect(result).toHaveProperty("amountOut");
      expect(result).toHaveProperty("fee");
      expect(result).toHaveProperty("priceAfter");
      expect(result).toHaveProperty("priceImpact");
      expect(result).toHaveProperty("originalPrice");

      // Check basic values
      expect(result.amountIn).toBe(1000);
      expect(result.fee).toBe(3); // 0.3% of 1000
      expect(result.amountOut).toBeGreaterThan(0);
      expect(result.priceImpact).toBeGreaterThan(0);
    });

    it("should handle swap with ticks data", () => {
      const result = estimateSwap0to1(
        mockPoolDataWithTickIndex,
        1000,
        mockTicks,
      );

      expect(result.amountIn).toBe(1000);
      expect(result.amountOut).toBeGreaterThan(0);
      expect(result.fee).toBe(3);
    });

    it("should handle swap with empty relevant ticks", () => {
      // All ticks below current tick index so no relevant ticks for 0->1 swap
      const lowTicks: Tick[] = [
        {
          tick_index: 7000,
          liquidity_net: "1000",
          liquidity_gross: "1000",
          fee_growth_outside0: "0",
          fee_growth_outside1: "0",
          initialized: true,
        },
        {
          tick_index: 7050,
          liquidity_net: "2000",
          liquidity_gross: "2000",
          fee_growth_outside0: "0",
          fee_growth_outside1: "0",
          initialized: true,
        },
      ];

      const result = estimateSwap0to1(
        mockPoolDataWithTickIndex,
        1000,
        lowTicks,
      );

      expect(result.amountIn).toBe(1000);
      expect(result.amountOut).toBeGreaterThan(0);
    });

    it("should handle large amounts that cross multiple ticks", () => {
      const result = estimateSwap0to1(
        mockPoolDataWithTickIndex,
        10000000,
        mockTicks,
      );

      expect(result.amountIn).toBe(10000000);
      expect(result.amountOut).toBeGreaterThan(0);
    });

    it("should handle zero amount", () => {
      const result = estimateSwap0to1(mockPoolData, 0, []);

      expect(result.amountIn).toBe(0);
      expect(result.amountOut).toBe(0);
      expect(result.fee).toBe(0);
    });

    it("should handle realistic USDT to VND swap", () => {
      // Test for 1 USDT input
      const result = estimateSwap0to1(mockUsdtVndPoolData, 1, mockUsdtVndTicks);

      // Check basic values
      expect(result.amountIn).toBe(1);
      expect(result.fee).toBe(0.003); // 0.3% of 1

      // Expected amount should be approximately 25,874 VND (with small price impact)
      // With improved math, this should be close to the ideal rate
      expect(result.amountOut).toBeCloseTo(25873.95 * 0.997, 0); // allowing some price impact

      // Price impact should be reasonable for a small swap
      expect(result.priceImpact).toBeLessThan(0.01); // less than 1%
    });

    it("should detect insufficient liquidity", () => {
      // Create a pool with zero liquidity
      const noLiquidityPool = {
        ...mockPoolData,
        tvl_in_token0: "0",
        tvl_in_token1: "0",
      };

      const result = estimateSwap0to1(noLiquidityPool, 1000, []) as SwapResult;

      // Should return an error property
      expect(result).toHaveProperty("error");
      expect(result.error).toContain("liquidity");
      expect(result.amountOut).toBe(0);
    });
  });

  describe("estimateSwap1to0", () => {
    it("should correctly estimate swap from token1 to token0", () => {
      const result = estimateSwap1to0(mockPoolData, 1000, []);

      // Check structure
      expect(result).toHaveProperty("amountIn");
      expect(result).toHaveProperty("amountOut");
      expect(result).toHaveProperty("fee");
      expect(result).toHaveProperty("priceAfter");
      expect(result).toHaveProperty("priceImpact");
      expect(result).toHaveProperty("originalPrice");

      // Check basic values
      expect(result.amountIn).toBe(1000);
      expect(result.fee).toBe(3); // 0.3% of 1000
      expect(result.amountOut).toBeGreaterThan(0);
      expect(result.priceImpact).toBeGreaterThan(0);
    });

    it("should handle swap with ticks data", () => {
      const result = estimateSwap1to0(
        mockPoolDataWithTickIndex,
        1000,
        mockTicks,
      );

      expect(result.amountIn).toBe(1000);
      expect(result.amountOut).toBeGreaterThan(0);
      expect(result.fee).toBe(3);
    });

    it("should handle swap with empty relevant ticks", () => {
      // All ticks above current tick index so no relevant ticks for 1->0 swap
      const highTicks: Tick[] = [
        {
          tick_index: 9000,
          liquidity_net: "1000",
          liquidity_gross: "1000",
          fee_growth_outside0: "0",
          fee_growth_outside1: "0",
          initialized: true,
        },
        {
          tick_index: 9050,
          liquidity_net: "2000",
          liquidity_gross: "2000",
          fee_growth_outside0: "0",
          fee_growth_outside1: "0",
          initialized: true,
        },
      ];

      const result = estimateSwap1to0(
        mockPoolDataWithTickIndex,
        1000,
        highTicks,
      );

      expect(result.amountIn).toBe(1000);
      expect(result.amountOut).toBeGreaterThan(0);
    });

    it("should handle large amounts that cross multiple ticks", () => {
      const result = estimateSwap1to0(
        mockPoolDataWithTickIndex,
        10000000,
        mockTicks,
      );

      expect(result.amountIn).toBe(10000000);
      expect(result.amountOut).toBeGreaterThan(0);
    });

    it("should handle zero amount", () => {
      const result = estimateSwap1to0(mockPoolData, 0, []);

      expect(result.amountIn).toBe(0);
      expect(result.amountOut).toBe(0);
      expect(result.fee).toBe(0);
    });

    it("should handle realistic VND to USDT swap", () => {
      // Test for 25,000 VND input (approximately 1 USDT)
      const result = estimateSwap1to0(
        mockUsdtVndPoolData,
        25000,
        mockUsdtVndTicks,
      );

      // Check basic values
      expect(result.amountIn).toBe(25000);
      expect(result.fee).toBe(75); // 0.3% of 25000

      // Expected amount should be approximately 0.9661 USDT (25000/25874.95*0.997)
      // With improved math, this should be close to the ideal rate
      expect(result.amountOut).toBeCloseTo((25000 / 25873.95) * 0.997, 4);

      // Price impact should be reasonable for a small swap
      expect(result.priceImpact).toBeLessThan(0.01); // less than 1%
    });

    it("should handle ratio-preserving swaps correctly", () => {
      // Test for 1 VND input, which should give approximately 1/25874 USDT (~0.0000386 USDT)
      const result = estimateSwap1to0(mockUsdtVndPoolData, 1, mockUsdtVndTicks);

      // Expected output should be close to 1/price
      const expectedOutput =
        (1 / parseFloat(mockUsdtVndPoolData.price)) * 0.997; // Apply fee
      expect(result.amountOut).toBeCloseTo(expectedOutput, 8);
    });

    it("should detect insufficient liquidity", () => {
      // Create a pool with zero liquidity
      const noLiquidityPool = {
        ...mockPoolData,
        tvl_in_token0: "0",
        tvl_in_token1: "0",
      };

      const result = estimateSwap1to0(noLiquidityPool, 1000, []) as SwapResult;

      // Should return an error property
      expect(result).toHaveProperty("error");
      expect(result.error).toContain("liquidity");
      expect(result.amountOut).toBe(0);
    });
  });

  describe("estimateSwapV3", () => {
    describe("USDT -> VND (zeroForOne = true)", () => {
      it("should correctly estimate swap with normal values", () => {
        const result = estimateSwapV3(mockPoolData, 1000, true);

        // Check structure
        expect(result).toHaveProperty("amountIn");
        expect(result).toHaveProperty("amountOut");
        expect(result).toHaveProperty("fee");
        expect(result).toHaveProperty("priceAfter");
        expect(result).toHaveProperty("priceImpact");
        expect(result).toHaveProperty("originalPrice");

        // Check basic values
        expect(result.amountIn).toBe(1000);
        expect(result.fee).toBe(3); // 0.3% of 1000

        // Verify the price impact is reasonable
        expect(result.priceImpact).toBeGreaterThan(0);
        expect(result.priceImpact).toBeLessThan(1); // Should be less than 100%

        // Verify amountOut is positive
        expect(result.amountOut).toBeGreaterThan(0);

        // Verify priceAfter is reasonable
        expect(result.priceAfter).toBeGreaterThan(0);
      });

      it("should handle zero amount", () => {
        const result = estimateSwapV3(mockPoolData, 0, true);

        // Basic checks
        expect(result.amountIn).toBe(0);
        expect(result.amountOut).toBe(0);
        expect(result.fee).toBe(0);
        expect(result.originalPrice).toBeGreaterThan(0);
      });

      it("should handle very large amounts", () => {
        const result = estimateSwapV3(mockPoolData, 1000000, true);

        // Basic checks
        expect(result.amountIn).toBe(1000000);
        expect(result.fee).toBe(3000); // 0.3% of 1000000
        expect(result.amountOut).toBeGreaterThan(0);
        expect(result.priceImpact).toBeGreaterThan(0);
      });

      it("should handle swap with ticks", () => {
        const result = estimateSwapV3(mockPoolData, 1000, true, mockTicks);

        expect(result.amountIn).toBe(1000);
        expect(result.amountOut).toBeGreaterThan(0);
      });

      it("should update price from current_tick_index when available", () => {
        const result = estimateSwapV3(mockPoolDataWithTickIndex, 1000, true);

        // The price should be calculated from the tick index
        expect(result.originalPrice).toBeCloseTo(2.25, 2); // approximately 1.0001^8100
      });

      it("should correctly estimate USDT to VND swap", () => {
        const result = estimateSwapV3(
          mockUsdtVndPoolData,
          1,
          true,
          mockUsdtVndTicks,
        );

        // Check for reasonable output amount (close to the price with fee)
        const expectedOutput =
          parseFloat(mockUsdtVndPoolData.price) * 1 * 0.997;
        expect(result.amountOut).toBeCloseTo(expectedOutput, 0);
      });
    });

    describe("VND -> USDT (zeroForOne = false)", () => {
      it("should correctly estimate swap with normal values", () => {
        const result = estimateSwapV3(mockPoolData, 1000, false);

        // Check structure
        expect(result).toHaveProperty("amountIn");
        expect(result).toHaveProperty("amountOut");
        expect(result).toHaveProperty("fee");
        expect(result).toHaveProperty("priceAfter");
        expect(result).toHaveProperty("priceImpact");
        expect(result).toHaveProperty("originalPrice");

        // Check basic values
        expect(result.amountIn).toBe(1000);
        expect(result.fee).toBe(3); // 0.3% of 1000

        // Verify the price impact is reasonable
        expect(result.priceImpact).toBeGreaterThan(0);
        expect(result.priceImpact).toBeLessThan(1); // Should be less than 100%

        // Verify amountOut is positive
        expect(result.amountOut).toBeGreaterThan(0);

        // Verify priceAfter is reasonable
        expect(result.priceAfter).toBeGreaterThan(0);
      });

      it("should handle zero amount", () => {
        const result = estimateSwapV3(mockPoolData, 0, false);

        // Basic checks
        expect(result.amountIn).toBe(0);
        expect(result.amountOut).toBe(0);
        expect(result.fee).toBe(0);
        expect(result.originalPrice).toBeGreaterThan(0);
      });

      it("should handle very large amounts", () => {
        const result = estimateSwapV3(mockPoolData, 1000000, false);

        // Basic checks
        expect(result.amountIn).toBe(1000000);
        expect(result.fee).toBe(3000); // 0.3% of 1000000
        expect(result.amountOut).toBeGreaterThan(0);
      });

      it("should handle swap with ticks", () => {
        const result = estimateSwapV3(mockPoolData, 1000, false, mockTicks);

        expect(result.amountIn).toBe(1000);
        expect(result.amountOut).toBeGreaterThan(0);
      });

      it("should correctly estimate VND to USDT swap", () => {
        const result = estimateSwapV3(
          mockUsdtVndPoolData,
          25000,
          false,
          mockUsdtVndTicks,
        );

        // Check for reasonable output amount (close to the price with fee)
        const expectedOutput =
          (25000 / parseFloat(mockUsdtVndPoolData.price)) * 0.997;
        expect(result.amountOut).toBeCloseTo(expectedOutput, 4);
      });

      it("should correctly calculate small VND to USDT swaps", () => {
        // Test for 1 VND to USDT
        const result = estimateSwapV3(
          mockUsdtVndPoolData,
          1,
          false,
          mockUsdtVndTicks,
        );

        // Expected output should be very small but not zero
        const expectedOutput =
          (1 / parseFloat(mockUsdtVndPoolData.price)) * 0.997;
        expect(result.amountOut).toBeCloseTo(expectedOutput, 8);

        // Value should be approximately 0.0000386 USDT
        expect(result.amountOut).toBeLessThan(0.0001);
        expect(result.amountOut).toBeGreaterThan(0);
      });
    });

    describe("Edge cases and error handling", () => {
      it("should handle pool data with string numbers correctly", () => {
        const poolDataWithLargeNumbers = {
          ...mockPoolData,
          sqrt_price: "1000000.5",
          tvl_in_token1: "999999999.99",
          price: "1000001000000.25",
        };

        const result = estimateSwapV3(poolDataWithLargeNumbers, 1000, true);

        expect(result.amountOut).toBeGreaterThan(0);
        expect(result.priceAfter).toBeGreaterThan(0);
        expect(result.priceImpact).toBeGreaterThan(0);
      });

      it("should handle very small fee percentage", () => {
        const poolDataWithSmallFee = {
          ...mockPoolData,
          fee_percentage: "0.0001", // 0.01%
        };

        const result = estimateSwapV3(poolDataWithSmallFee, 1000, true);

        expect(result.fee).toBe(0.1); // 0.01% of 1000
        expect(result.amountOut).toBeGreaterThan(0);
      });

      it("should detect insufficient liquidity for zeroForOne=true", () => {
        // Mock an error by triggering insufficient liquidity
        const badPoolData = {
          ...mockPoolData,
          tvl_in_token0: "0",
          tvl_in_token1: "0",
        };

        const result = estimateSwapV3(badPoolData, 1000, true) as SwapResult;

        // Should have error property
        expect(result).toHaveProperty("error");
        expect(result.error).toContain("liquidity");
        expect(result.amountOut).toBe(0);
      });

      it("should detect insufficient liquidity for zeroForOne=false", () => {
        // Mock an error by triggering insufficient liquidity
        const badPoolData = {
          ...mockPoolData,
          tvl_in_token0: "0",
          tvl_in_token1: "0",
        };

        const result = estimateSwapV3(badPoolData, 1000, false) as SwapResult;

        // Should have error property
        expect(result).toHaveProperty("error");
        expect(result.error).toContain("liquidity");
        expect(result.amountOut).toBe(0);
      });

      it("should calculate tick index from price when not provided", () => {
        // Create a pool data object without current_tick_index
        const poolDataNoTickIndex = { ...mockPoolData };

        const result = estimateSwapV3(poolDataNoTickIndex, 1000, true);

        expect(result.originalPrice).toBeCloseTo(
          parseFloat(mockPoolData.price),
          2,
        );
        expect(result.amountOut).toBeGreaterThan(0);
      });
    });
  });

  describe("Exact Output Mode (exactOutput = true)", () => {
    describe("USDT -> VND (zeroForOne = true)", () => {
      it("should correctly calculate required input for desired output", () => {
        const desiredOutput = 2000; // Want 2000 VND
        const result = estimateSwapV3(
          mockPoolData,
          desiredOutput,
          true,
          [],
          true,
        );

        // Check structure - exact output should return amountIn as the calculation result
        expect(result).toHaveProperty("amountIn");
        expect(result).toHaveProperty("amountOut");
        expect(result).toHaveProperty("fee");
        expect(result).toHaveProperty("priceAfter");
        expect(result).toHaveProperty("priceImpact");
        expect(result).toHaveProperty("originalPrice");

        // In exact output mode, amountOut should match the desired output
        expect(result.amountOut).toBe(desiredOutput);

        // amountIn should be calculated based on price and fees
        expect(result.amountIn).toBeGreaterThan(0);

        // Fee should be calculated based on amountIn
        expect(result.fee).toBeGreaterThan(0);
        expect(result.fee).toBeCloseTo(result.amountIn * 0.003, 5);
      });

      it("should handle real-world USDT-VND exact output calculation", () => {
        const desiredVnd = 1500000; // Want 1.5M VND
        const result = estimateSwapV3(
          mockUsdtVndPoolData,
          desiredVnd,
          true,
          mockUsdtVndTicks,
          true,
        );

        // Should calculate required USDT input
        expect(result.amountOut).toBe(desiredVnd);
        expect(result.amountIn).toBeGreaterThan(0);

        // Required input should be approximately desiredVnd / price with fees
        const expectedInput =
          desiredVnd / parseFloat(mockUsdtVndPoolData.price) / 0.997;
        expect(result.amountIn).toBeCloseTo(expectedInput, 2);
      });

      it("should handle exact output with ticks", () => {
        const desiredOutput = 2000;
        const result = estimateSwapV3(
          mockPoolDataWithTickIndex,
          desiredOutput,
          true,
          mockTicks,
          true,
        );

        expect(result.amountOut).toBe(desiredOutput);
        expect(result.amountIn).toBeGreaterThan(0);
        expect(result.fee).toBeGreaterThan(0);
      });

      it("should handle small desired outputs", () => {
        const desiredOutput = 0.1; // Very small amount
        const result = estimateSwapV3(
          mockPoolData,
          desiredOutput,
          true,
          [],
          true,
        );

        expect(result.amountOut).toBe(desiredOutput);
        expect(result.amountIn).toBeGreaterThan(0);
        expect(result.amountIn).toBeLessThan(1);
      });

      it("should handle large desired outputs", () => {
        const desiredOutput = 1000000; // Large amount
        const result = estimateSwapV3(
          mockPoolData,
          desiredOutput,
          true,
          [],
          true,
        );

        expect(result.amountOut).toBe(desiredOutput);
        expect(result.amountIn).toBeGreaterThan(0);
        expect(result.priceImpact).toBeGreaterThan(0);
      });
    });

    describe("VND -> USDT (zeroForOne = false)", () => {
      it("should correctly calculate required input for desired output", () => {
        const desiredOutput = 50; // Want 50 USDT
        const result = estimateSwapV3(
          mockPoolData,
          desiredOutput,
          false,
          [],
          true,
        );

        // Check structure
        expect(result).toHaveProperty("amountIn");
        expect(result).toHaveProperty("amountOut");
        expect(result).toHaveProperty("fee");
        expect(result).toHaveProperty("priceAfter");
        expect(result).toHaveProperty("priceImpact");
        expect(result).toHaveProperty("originalPrice");

        // In exact output mode, amountOut should match the desired output
        expect(result.amountOut).toBe(desiredOutput);

        // amountIn should be calculated based on price and fees
        expect(result.amountIn).toBeGreaterThan(0);

        // Fee should be calculated based on amountIn
        expect(result.fee).toBeGreaterThan(0);
        expect(result.fee).toBeCloseTo(result.amountIn * 0.003, 5);
      });

      it("should handle real-world VND-USDT exact output calculation", () => {
        const desiredUsdt = 50; // Want 50 USDT
        const result = estimateSwapV3(
          mockUsdtVndPoolData,
          desiredUsdt,
          false,
          mockUsdtVndTicks,
          true,
        );

        // Should calculate required VND input
        expect(result.amountOut).toBe(desiredUsdt);
        expect(result.amountIn).toBeGreaterThan(0);

        // Required input should be approximately desiredUsdt * price with fees
        const expectedInput =
          (desiredUsdt * parseFloat(mockUsdtVndPoolData.price)) / 0.997;
        // Allow reasonable tolerance for price impact and tick processing
        const tolerance = expectedInput * 0.01; // 1% tolerance
        expect(Math.abs(result.amountIn - expectedInput)).toBeLessThan(
          tolerance,
        );
      });

      it("should handle exact output with ticks", () => {
        const desiredOutput = 10;
        const result = estimateSwapV3(
          mockPoolDataWithTickIndex,
          desiredOutput,
          false,
          mockTicks,
          true,
        );

        expect(result.amountOut).toBe(desiredOutput);
        expect(result.amountIn).toBeGreaterThan(0);
        expect(result.fee).toBeGreaterThan(0);
      });

      it("should handle small desired USDT outputs", () => {
        const desiredOutput = 0.001; // Very small USDT amount
        const result = estimateSwapV3(
          mockUsdtVndPoolData,
          desiredOutput,
          false,
          [],
          true,
        );

        expect(result.amountOut).toBe(desiredOutput);
        expect(result.amountIn).toBeGreaterThan(0);
      });
    });

    describe("Exact Output Error Handling", () => {
      it("should handle zero desired output", () => {
        const result = estimateSwapV3(mockPoolData, 0, true, [], true);

        expect(result.amountIn).toBe(0);
        expect(result.amountOut).toBe(0);
        expect(result.fee).toBe(0);
        expect(result.originalPrice).toBeGreaterThan(0);
      });

      it("should detect insufficient liquidity in exact output mode", () => {
        const badPoolData = {
          ...mockPoolData,
          tvl_in_token0: "0",
          tvl_in_token1: "0",
        };

        const result = estimateSwapV3(
          badPoolData,
          1000,
          true,
          [],
          true,
        ) as SwapResult;

        expect(result).toHaveProperty("error");
        expect(result.error).toContain("liquidity");
        expect(result.amountIn).toBe(0);
      });

      it("should handle unrealistic desired outputs gracefully", () => {
        // Try to get more tokens than available in the pool
        const unrealisticOutput = parseFloat(mockPoolData.tvl_in_token1) * 2;
        const result = estimateSwapV3(
          mockPoolData,
          unrealisticOutput,
          true,
          [],
          true,
        ) as SwapResult;

        // Should either succeed with high price impact or fail with error
        if (result.error) {
          expect(result.error).toBeDefined();
          expect(result.amountIn).toBe(0);
        } else {
          expect(result.priceImpact).toBeGreaterThan(0.5); // >50% price impact
        }
      });
    });

    describe("Exact Input vs Exact Output Consistency", () => {
      it("should have consistent pricing between exact input and exact output", () => {
        // Test exact input first
        const inputAmount = 100;
        const exactInputResult = estimateSwapV3(
          mockPoolData,
          inputAmount,
          true,
          [],
          false,
        );

        // Then test exact output with the result from exact input
        const exactOutputResult = estimateSwapV3(
          mockPoolData,
          exactInputResult.amountOut,
          true,
          [],
          true,
        );

        // The required input for exact output should be close to original input
        // Allow some tolerance due to price impact and rounding
        expect(exactOutputResult.amountIn).toBeCloseTo(inputAmount, 0);
        expect(exactOutputResult.amountOut).toBeCloseTo(
          exactInputResult.amountOut,
          2,
        );
      });

      it("should have consistent USDT-VND pricing", () => {
        const inputUsdt = 50;
        const exactInputResult = estimateSwapV3(
          mockUsdtVndPoolData,
          inputUsdt,
          true,
          mockUsdtVndTicks,
          false,
        );

        const exactOutputResult = estimateSwapV3(
          mockUsdtVndPoolData,
          exactInputResult.amountOut,
          true,
          mockUsdtVndTicks,
          true,
        );

        // Should be close but not exact due to price impact
        expect(exactOutputResult.amountIn).toBeCloseTo(inputUsdt, 1);
      });

      it("should have consistent VND-USDT pricing", () => {
        const inputVnd = 1000000;
        const exactInputResult = estimateSwapV3(
          mockUsdtVndPoolData,
          inputVnd,
          false,
          mockUsdtVndTicks,
          false,
        );

        const exactOutputResult = estimateSwapV3(
          mockUsdtVndPoolData,
          exactInputResult.amountOut,
          false,
          mockUsdtVndTicks,
          true,
        );

        // Should be close but not exact due to price impact
        expect(exactOutputResult.amountIn).toBeCloseTo(inputVnd, 0);
      });
    });
  });
});
