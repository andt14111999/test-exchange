import { BigNumber } from "bignumber.js";
import {
  formatCurrency,
  formatFiatAmount,
  formatLiquidity,
  formatDate,
} from "@/lib/utils/format";

// Mock Intl.NumberFormat
const mockFormat = jest.fn();
const mockNumberFormat = jest.fn(() => ({
  format: mockFormat,
}));

// Mock Intl.DateTimeFormat
const mockDateFormat = jest.fn();
const mockDateTimeFormat = jest.fn(() => ({
  format: mockDateFormat,
}));

// Store original Intl constructors
const originalNumberFormat = Intl.NumberFormat;
const originalDateTimeFormat = Intl.DateTimeFormat;

beforeAll(() => {
  global.Intl.NumberFormat =
    mockNumberFormat as unknown as typeof Intl.NumberFormat;
  global.Intl.DateTimeFormat =
    mockDateTimeFormat as unknown as typeof Intl.DateTimeFormat;
});

afterAll(() => {
  global.Intl.NumberFormat = originalNumberFormat;
  global.Intl.DateTimeFormat = originalDateTimeFormat;
});

beforeEach(() => {
  mockFormat.mockClear();
  mockNumberFormat.mockClear();
  mockDateFormat.mockClear();
  mockDateTimeFormat.mockClear();
});

describe("Format Utils", () => {
  describe("formatCurrency", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => value.toString());
    });

    it("should format number with default options", () => {
      const result = formatCurrency(1000.5, "USD");

      expect(mockNumberFormat).toHaveBeenCalledWith("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
      expect(result).toBe("$1000.5");
    });

    it("should format string amount", () => {
      const result = formatCurrency("1000.5", "USD");
      expect(result).toBe("$1000.5");
    });

    it("should format BigNumber amount", () => {
      const result = formatCurrency(new BigNumber("1000.5"), "USD");
      expect(result).toBe("$1000.5");
    });

    it("should handle custom decimals", () => {
      const result = formatCurrency(1000.5678, "USD", { decimals: 4 });

      expect(mockNumberFormat).toHaveBeenCalledWith("en-US", {
        minimumFractionDigits: 4,
        maximumFractionDigits: 4,
      });
      expect(result).toBe("$1000.5678");
    });

    it("should hide symbol when showSymbol is false", () => {
      const result = formatCurrency(1000.5, "USD", { showSymbol: false });
      expect(result).toBe("1000.5");
    });

    it("should handle different currencies", () => {
      expect(formatCurrency(1000, "VND")).toBe("₫1000");
      expect(formatCurrency(1000, "PHP")).toBe("₱1000");
      expect(formatCurrency(1000, "NGN")).toBe("₦1000");
      expect(formatCurrency(1000, "USDT")).toBe("$1000");
    });

    it("should handle unknown currency", () => {
      const result = formatCurrency(1000, "XXX");
      expect(result).toBe("1000");
    });

    it("should return 0 for invalid input", () => {
      mockFormat.mockImplementation(() => "NaN");
      const result = formatCurrency("invalid", "USD");
      expect(result).toBe("$NaN");
    });
  });

  describe("formatFiatAmount", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => value.toString());
    });

    it("should format amount with currency symbol", () => {
      const result = formatFiatAmount(1000, "VND");

      expect(mockNumberFormat).toHaveBeenCalledWith("en-US", {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      });
      expect(result).toBe("₫1000");
    });

    it("should handle different currencies", () => {
      expect(formatFiatAmount(1000, "PHP")).toBe("₱1000");
      expect(formatFiatAmount(1000, "NGN")).toBe("₦1000");
      expect(formatFiatAmount(1000, "USDT")).toBe("$1000");
    });

    it("should handle unknown currency", () => {
      const result = formatFiatAmount(1000, "XXX");
      expect(result).toBe("1000");
    });
  });

  describe("formatLiquidity", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => value.toString());
    });

    it("should format number with 2 decimal places", () => {
      const result = formatLiquidity(1000.5678);

      expect(mockNumberFormat).toHaveBeenCalledWith("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
      expect(result).toBe("1000.5678");
    });

    it("should handle string input", () => {
      const result = formatLiquidity("1000.5678");
      expect(result).toBe("1000.5678");
    });
  });

  describe("formatDate", () => {
    beforeEach(() => {
      mockDateFormat.mockImplementation(() => "Jan 1, 2024, 12:00 PM");
    });

    it("should format date with correct options", () => {
      const date = new Date("2024-01-01T12:00:00");
      const result = formatDate(date);

      expect(mockDateTimeFormat).toHaveBeenCalledWith("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
      expect(result).toBe("Jan 1, 2024, 12:00 PM");
    });
  });
});
