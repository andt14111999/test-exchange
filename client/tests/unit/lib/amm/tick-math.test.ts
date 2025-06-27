import { TickMath } from "@/lib/amm/tick-math";

// Mock Intl.NumberFormat
const mockFormat = jest.fn((x) => x.toString());
const mockNumberFormat = {
  format: mockFormat,
  resolvedOptions: jest.fn(),
  formatToParts: jest.fn(),
  formatRange: jest.fn(),
  formatRangeToParts: jest.fn(),
};

// Create a properly typed mock constructor
const NumberFormatMock = jest.fn(() => mockNumberFormat) as jest.Mock<
  typeof mockNumberFormat
> & {
  supportedLocalesOf: jest.Mock;
};
NumberFormatMock.supportedLocalesOf = jest.fn();

// Type-safe way to mock global Intl.NumberFormat
(global.Intl.NumberFormat as unknown as typeof NumberFormatMock) =
  NumberFormatMock;

describe("TickMath", () => {
  beforeEach(() => {
    mockFormat.mockClear();
    NumberFormatMock.mockClear();
  });

  describe("Constants", () => {
    it("should have correct constant values", () => {
      expect(TickMath.MIN_TICK).toBe(-887272);
      expect(TickMath.MAX_TICK).toBe(887272);
      expect(TickMath.TICK_MULTIPLIER).toBe(1.0001);
    });
  });

  describe("priceToTick", () => {
    it("should convert price to tick correctly", () => {
      const price = 2;
      const tick = TickMath.priceToTick(price);
      expect(tick).toBeLessThanOrEqual(TickMath.MAX_TICK);
      expect(tick).toBeGreaterThanOrEqual(TickMath.MIN_TICK);
      expect(typeof tick).toBe("number");
    });

    it("should handle invalid price", () => {
      const tick = TickMath.priceToTick(0);
      expect(tick).toBe(0);
    });

    it("should handle negative price", () => {
      const tick = TickMath.priceToTick(-1);
      expect(tick).toBe(0);
    });

    it("should handle extreme prices", () => {
      const highTick = TickMath.priceToTick(1e100);
      expect(highTick).toBe(TickMath.MAX_TICK);

      const lowTick = TickMath.priceToTick(1e-100);
      expect(lowTick).toBe(TickMath.MIN_TICK);
    });
  });

  describe("tickToPrice", () => {
    it("should convert tick to price correctly", () => {
      const tick = 100;
      const price = TickMath.tickToPrice(tick);
      expect(price).toBeGreaterThan(0);
      expect(typeof price).toBe("number");
    });

    it("should handle boundary ticks", () => {
      const maxPrice = TickMath.tickToPrice(TickMath.MAX_TICK);
      expect(maxPrice).toBeGreaterThan(0);
      expect(isFinite(maxPrice)).toBe(true);

      const minPrice = TickMath.tickToPrice(TickMath.MIN_TICK);
      expect(minPrice).toBeGreaterThan(0);
      expect(isFinite(minPrice)).toBe(true);
    });

    it("should handle error cases", () => {
      const price = TickMath.tickToPrice(NaN);
      expect(isNaN(price)).toBe(true); // NaN is the correct mathematical result
    });
  });

  describe("roundToTickSpacing", () => {
    it("should round to nearest tick spacing", () => {
      expect(TickMath.roundToTickSpacing(105, 10)).toBe(100); // Rounds to nearest
      expect(TickMath.roundToTickSpacing(104, 10)).toBe(100);
    });

    it("should handle negative ticks", () => {
      expect(TickMath.roundToTickSpacing(-105, 10)).toBe(-100); // Rounds to nearest
      expect(TickMath.roundToTickSpacing(-104, 10)).toBe(-100);
    });

    it("should handle zero tick", () => {
      expect(TickMath.roundToTickSpacing(0, 10)).toBe(0);
    });

    it("should handle invalid tick spacing", () => {
      expect(TickMath.roundToTickSpacing(100, 0)).toBe(100);
      expect(TickMath.roundToTickSpacing(100, -1)).toBe(100);
    });

    it("should respect roundUp parameter", () => {
      expect(TickMath.roundToTickSpacing(105, 10, true)).toBe(110);
      expect(TickMath.roundToTickSpacing(101, 10, true)).toBe(110);
    });
  });

  describe("calculateTickRange", () => {
    it("should calculate tick range correctly", () => {
      const result = TickMath.calculateTickRange(100, 5, 10);
      expect(result).toEqual({
        tickLower: expect.any(Number),
        tickUpper: expect.any(Number),
      });
      expect(result.tickUpper).toBeGreaterThan(result.tickLower);
    });

    it("should handle invalid current price", () => {
      const result = TickMath.calculateTickRange(-100, 5, 10);
      expect(result.tickUpper).toBeGreaterThan(result.tickLower);
      expect(result.tickUpper - result.tickLower).toBeGreaterThanOrEqual(10);
    });

    it("should ensure minimum tick spacing", () => {
      const result = TickMath.calculateTickRange(100, 0.0001, 10);
      expect(result.tickUpper - result.tickLower).toBeGreaterThanOrEqual(10);
    });
  });

  describe("isInvalidTickRange", () => {
    it("should detect invalid tick ranges", () => {
      expect(TickMath.isInvalidTickRange(100, 90)).toBe(true);
      expect(TickMath.isInvalidTickRange(100, 100)).toBe(true);
      expect(TickMath.isInvalidTickRange(90, 100)).toBe(false);
    });
  });

  describe("fixInvalidTickRange", () => {
    it("should fix invalid tick ranges", () => {
      const result = TickMath.fixInvalidTickRange(100, 90, 10);
      expect(result).toEqual({
        tickLower: 100,
        tickUpper: 110,
      });
    });

    it("should not modify valid tick ranges", () => {
      const result = TickMath.fixInvalidTickRange(90, 100, 10);
      expect(result).toEqual({
        tickLower: 90,
        tickUpper: 100,
      });
    });
  });

  describe("calculateToken1Amount", () => {
    it("should calculate token1 amount correctly", () => {
      expect(TickMath.calculateToken1Amount(100, 2)).toBe(200);
    });

    it("should handle invalid inputs", () => {
      expect(TickMath.calculateToken1Amount(0, 2)).toBe(0);
      expect(TickMath.calculateToken1Amount(-1, 2)).toBe(0);
      expect(TickMath.calculateToken1Amount(100, 0)).toBe(0);
      expect(TickMath.calculateToken1Amount(100, -1)).toBe(0);
      expect(TickMath.calculateToken1Amount(NaN, 2)).toBe(0);
      expect(TickMath.calculateToken1Amount(100, NaN)).toBe(0);
    });
  });

  describe("calculateToken0Amount", () => {
    it("should calculate token0 amount correctly", () => {
      expect(TickMath.calculateToken0Amount(200, 2)).toBe(100);
    });

    it("should handle invalid inputs", () => {
      expect(TickMath.calculateToken0Amount(0, 2)).toBe(0);
      expect(TickMath.calculateToken0Amount(-1, 2)).toBe(0);
      expect(TickMath.calculateToken0Amount(100, 0)).toBe(0);
      expect(TickMath.calculateToken0Amount(100, -1)).toBe(0);
      expect(TickMath.calculateToken0Amount(NaN, 2)).toBe(0);
      expect(TickMath.calculateToken0Amount(100, NaN)).toBe(0);
    });
  });

  describe("formatPrice", () => {
    it("should format price correctly", () => {
      TickMath.formatPrice(1234.56);
      expect(NumberFormatMock).toHaveBeenCalled();
      expect(mockFormat).toHaveBeenCalledWith(1235); // Rounded
    });

    it("should handle invalid prices", () => {
      expect(TickMath.formatPrice(-1)).toBe("0");
      expect(TickMath.formatPrice(Infinity)).toBe("0");
      expect(TickMath.formatPrice(NaN)).toBe("0");
    });

    it("should handle error in formatting", () => {
      mockFormat.mockImplementationOnce(() => {
        throw new Error("Mock format error");
      });
      expect(TickMath.formatPrice(1234.56)).toBe("0");
    });
  });
});
