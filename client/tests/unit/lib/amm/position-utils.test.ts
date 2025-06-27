import { BigNumber } from "bignumber.js";
import { TickMath } from "@/lib/amm/tick-math";
import {
  calculateTickRange,
  calculateTokenAmounts,
  createAMMPosition,
  validatePosition,
  formatNumberWithCommas,
  parseFormattedNumber,
  formatDisplayPrice,
  formatDisplayNumberWithPrecision,
} from "@/lib/amm/position-utils";
import { FormattedPool } from "@/lib/api/pools";
import { LiquidityCalculator } from "@/lib/amm/liquidity_calculator";

// Mock TickMath
jest.mock("@/lib/amm/tick-math", () => ({
  TickMath: {
    tickToPrice: jest.fn(),
    priceToTick: jest.fn(),
    roundToTickSpacing: jest.fn(),
  },
}));

// Mock LiquidityCalculator
jest.mock("@/lib/amm/liquidity_calculator", () => ({
  LiquidityCalculator: {
    calculateAmounts: jest.fn(),
  },
}));

// Mock console.log
const mockConsoleLog = jest.spyOn(console, "log").mockImplementation(() => {});

// Store original Intl.NumberFormat
const OriginalIntl = global.Intl;

// Mock Intl.NumberFormat
const mockFormat = jest.fn();
const mockNumberFormat = jest.fn(() => ({
  format: mockFormat,
})) as jest.Mock;

describe("Position Utils", () => {
  const mockPool: FormattedPool = {
    id: 1,
    name: "USDT/VND",
    pair: "USDT/VND",
    token0: "USDT",
    token1: "VND",
    currentTick: 0,
    tickSpacing: 10,
    fee: 0.003,
    price: new BigNumber(25000),
    sqrtPriceX96: new BigNumber(25000),
    liquidity: new BigNumber("1000000"),
    apr: 0.1,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // Mock Intl.NumberFormat
    global.Intl = {
      ...OriginalIntl,
      NumberFormat: mockNumberFormat as unknown as typeof Intl.NumberFormat,
    };
  });

  afterEach(() => {
    // Restore original Intl
    global.Intl = OriginalIntl;
  });

  describe("calculateTickRange", () => {
    beforeEach(() => {
      (TickMath.tickToPrice as jest.Mock).mockReturnValue(100);
      (TickMath.priceToTick as jest.Mock).mockImplementation((price) => price);
      (TickMath.roundToTickSpacing as jest.Mock).mockImplementation(
        (tick, spacing, up = false) => (up ? tick + 1 : tick),
      );
    });

    it("should calculate tick range correctly with default percentage", () => {
      const result = calculateTickRange(0, 10);
      expect(result).toEqual({
        tickLower: 95,
        tickUpper: 106,
      });
      expect(mockConsoleLog).not.toHaveBeenCalled();
    });

    it("should calculate tick range correctly with custom percentage", () => {
      const result = calculateTickRange(0, 10, 10);
      expect(result.tickLower).toBe(90);
      expect(result.tickUpper).toBeCloseTo(111, 0);
    });

    it("should ensure tickLower < tickUpper", () => {
      (TickMath.roundToTickSpacing as jest.Mock).mockReturnValue(100);
      const result = calculateTickRange(0, 10);
      expect(result.tickLower).toBeLessThan(result.tickUpper);
    });
  });

  describe("calculateTokenAmounts", () => {
    beforeEach(() => {
      (LiquidityCalculator.calculateAmounts as jest.Mock).mockReturnValue({
        amount0: new BigNumber(100),
        amount1: new BigNumber(200),
      });
    });

    it("should calculate token amounts correctly for token0", () => {
      const result = calculateTokenAmounts(mockPool, -10, 10, "token0", 100);
      expect(result).toEqual({
        token0: "100",
        token1: "200",
      });
      expect(LiquidityCalculator.calculateAmounts).toHaveBeenCalledWith({
        tickLower: -10,
        tickUpper: 10,
        currentTick: 0,
        amount0: 100,
        amount1: null,
        token0Symbol: "USDT",
        token1Symbol: "VND",
      });
    });

    it("should calculate token amounts correctly for token1", () => {
      const result = calculateTokenAmounts(mockPool, -10, 10, "token1", 100);
      expect(result).toEqual({
        token0: "100",
        token1: "200",
      });
      expect(LiquidityCalculator.calculateAmounts).toHaveBeenCalledWith({
        tickLower: -10,
        tickUpper: 10,
        currentTick: 0,
        amount0: null,
        amount1: 100,
        token0Symbol: "USDT",
        token1Symbol: "VND",
      });
    });
  });

  describe("createAMMPosition", () => {
    it("should create position with default slippage", () => {
      const result = createAMMPosition(mockPool, -10, 10, "100", "200");
      expect(result).toEqual({
        pool_pair: "USDT/VND",
        tick_lower_index: -10,
        tick_upper_index: 10,
        amount0_initial: "100",
        amount1_initial: "200",
        slippage: 100,
      });
    });

    it("should create position with custom slippage", () => {
      const result = createAMMPosition(mockPool, -10, 10, "100", "200", 50);
      expect(result).toEqual({
        pool_pair: "USDT/VND",
        tick_lower_index: -10,
        tick_upper_index: 10,
        amount0_initial: "100",
        amount1_initial: "200",
        slippage: 50,
      });
    });
  });

  describe("validatePosition", () => {
    it("should validate valid position", () => {
      const result = validatePosition(mockPool, -10, 10, "100", "200");
      expect(result).toEqual({
        isValid: true,
        errors: {},
      });
    });

    it("should handle null pool", () => {
      const result = validatePosition(null, -10, 10, "100", "200");
      expect(result).toEqual({
        isValid: false,
        errors: { tickRange: "Pool không tồn tại" },
      });
    });

    it("should validate tick range", () => {
      const result = validatePosition(mockPool, 10, -10, "100", "200");
      expect(result.isValid).toBe(false);
      expect(result.errors.tickRange).toBe(
        "Giá thấp nhất phải nhỏ hơn giá cao nhất",
      );
    });

    it("should validate token amounts", () => {
      const result = validatePosition(mockPool, -10, 10, "0", "-200");
      expect(result.isValid).toBe(false);
      expect(result.errors.token0).toBe("Số lượng token không hợp lệ");
      expect(result.errors.token1).toBe("Số lượng token không hợp lệ");
    });
  });

  describe("formatNumberWithCommas", () => {
    it("should format integer numbers", () => {
      mockFormat.mockImplementation((value) =>
        value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ","),
      );
      expect(formatNumberWithCommas("1234567")).toBe("1,234,567");
    });

    it("should format decimal numbers", () => {
      mockFormat.mockImplementation((value) => {
        const [integer, decimal] = value.toString().split(".");
        return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ",")}.${decimal}`;
      });
      expect(formatNumberWithCommas("1234567.89")).toBe("1,234,567.89");
    });

    it("should handle numbers with existing commas", () => {
      expect(formatNumberWithCommas("1,234,567.89")).toBe("1,234,567.89");
    });
  });

  describe("parseFormattedNumber", () => {
    it("should parse formatted numbers", () => {
      expect(parseFormattedNumber("1,234,567.89")).toBe(1234567.89);
    });

    it("should handle invalid numbers", () => {
      expect(parseFormattedNumber("invalid")).toBe(0);
    });
  });

  describe("formatDisplayPrice", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => value.toLocaleString("en-US"));
    });

    it("should format zero", () => {
      expect(formatDisplayPrice(0)).toBe("0");
    });

    it("should format large numbers", () => {
      mockFormat.mockReturnValue("1,234.57");
      expect(formatDisplayPrice(1234.5678, true)).toBe("1,234.57");
    });

    it("should format small numbers", () => {
      mockFormat.mockReturnValue("0.123457");
      expect(formatDisplayPrice(0.1234567)).toBe("0.123457");
    });

    it("should respect decimals parameter", () => {
      mockFormat.mockReturnValue("1,234.5678");
      expect(formatDisplayPrice(1234.5678, false, 4)).toBe("1,234.5678");
    });
  });

  describe("formatDisplayNumberWithPrecision", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => {
        const numValue = parseFloat(value);
        if (isNaN(numValue)) return "0";

        // Get the options passed to NumberFormat
        const options =
          mockNumberFormat.mock.calls[
            mockNumberFormat.mock.calls.length - 1
          ][1] || {};
        const maxDecimals = options.maximumFractionDigits ?? 4;
        const minDecimals = options.minimumFractionDigits ?? 0;
        const useGrouping = options.useGrouping ?? true;

        // Convert to string and split into parts
        const [integerStr, decimalStr = ""] = numValue.toString().split(".");

        // Format integer part with grouping if needed
        const integerPart = useGrouping
          ? integerStr.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
          : integerStr;

        // Handle decimal part
        let decimalPart = decimalStr.slice(0, maxDecimals);
        if (decimalPart.length < minDecimals) {
          decimalPart = decimalPart.padEnd(minDecimals, "0");
        }

        // Return formatted number
        return decimalPart ? `${integerPart}.${decimalPart}` : integerPart;
      });
    });

    it("should format string numbers", () => {
      expect(formatDisplayNumberWithPrecision("1234.5678")).toBe("1,234.5678");
    });

    it("should format numeric values", () => {
      expect(formatDisplayNumberWithPrecision(1234.5678)).toBe("1,234.5678");
    });

    it("should handle invalid numbers", () => {
      expect(formatDisplayNumberWithPrecision("invalid")).toBe("0");
    });

    it("should handle integers", () => {
      expect(formatDisplayNumberWithPrecision(1234)).toBe("1,234");
    });

    it("should respect maxDecimals parameter", () => {
      expect(formatDisplayNumberWithPrecision("1234.56789", 2)).toBe(
        "1,234.57",
      );
    });

    it("should handle numbers with existing commas", () => {
      expect(formatDisplayNumberWithPrecision("1,234.5678")).toBe("1,234.5678");
    });

    it("should handle very small decimals", () => {
      expect(formatDisplayNumberWithPrecision("0.0001234")).toBe("0.0001");
    });

    it("should handle very large numbers", () => {
      expect(formatDisplayNumberWithPrecision("1234567.89")).toBe(
        "1,234,567.89",
      );
    });
  });
});
