import {
  cn,
  formatFiatAmount,
  formatUSDTAmount,
  formatWalletAddress,
  calculateTimeLeft,
  generateMockId,
  isValidAmount,
  calculateFiatAmount,
  formatNumber,
} from "@/lib/utils/index";

// Mock Intl.NumberFormat
const mockFormat = jest.fn();
const mockNumberFormat = jest.fn(() => ({
  format: mockFormat,
}));

// Store original Intl constructor
const originalNumberFormat = Intl.NumberFormat;

beforeAll(() => {
  global.Intl.NumberFormat =
    mockNumberFormat as unknown as typeof Intl.NumberFormat;
});

afterAll(() => {
  global.Intl.NumberFormat = originalNumberFormat;
});

beforeEach(() => {
  mockFormat.mockClear();
  mockNumberFormat.mockClear();
});

describe("Utils", () => {
  describe("cn", () => {
    it("should merge class names correctly", () => {
      expect(cn("class1", "class2")).toBe("class1 class2");
      expect(cn("class1", { class2: true, class3: false })).toBe(
        "class1 class2",
      );
      expect(cn("p-4 bg-red", "text-white", "hover:bg-blue")).toBe(
        "p-4 bg-red text-white hover:bg-blue",
      );
    });
  });

  describe("formatFiatAmount", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => `${value}`);
    });

    it("should format amount with currency", () => {
      const result = formatFiatAmount(1000, "USD");
      expect(mockNumberFormat).toHaveBeenCalledWith(undefined, {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      });
      expect(result).toBe("1000");
    });

    it("should handle different currencies", () => {
      formatFiatAmount(1000, "EUR");
      expect(mockNumberFormat).toHaveBeenCalledWith(undefined, {
        style: "currency",
        currency: "EUR",
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      });
    });
  });

  describe("formatUSDTAmount", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => value.toFixed(2));
    });

    it("should format USDT amount with 2 decimal places", () => {
      const result = formatUSDTAmount(1000.567);
      expect(mockNumberFormat).toHaveBeenCalledWith("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
      expect(result).toBe("1000.57 USDT");
    });

    it("should handle whole numbers", () => {
      const result = formatUSDTAmount(1000);
      expect(result).toBe("1000.00 USDT");
    });
  });

  describe("formatWalletAddress", () => {
    it("should format wallet address correctly", () => {
      const address = "0x1234567890abcdef1234567890abcdef12345678";
      const result = formatWalletAddress(address);
      expect(result).toBe("0x1234...5678");
    });

    it("should handle empty address", () => {
      expect(formatWalletAddress("")).toBe("");
    });
  });

  describe("calculateTimeLeft", () => {
    beforeEach(() => {
      jest.useFakeTimers();
      jest.setSystemTime(new Date("2024-01-01T12:00:00Z"));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it("should calculate remaining time in seconds", () => {
      const expiresAt = new Date("2024-01-01T12:01:00Z"); // 1 minute later
      const result = calculateTimeLeft(expiresAt);
      expect(result).toBe(60);
    });

    it("should return 0 for past dates", () => {
      const expiresAt = new Date("2024-01-01T11:59:00Z"); // 1 minute before
      const result = calculateTimeLeft(expiresAt);
      expect(result).toBe(0);
    });
  });

  describe("generateMockId", () => {
    it("should generate a string id", () => {
      const result = generateMockId();
      expect(typeof result).toBe("string");
      expect(result.length).toBeGreaterThan(0);
    });

    it("should generate unique ids", () => {
      const id1 = generateMockId();
      const id2 = generateMockId();
      expect(id1).not.toBe(id2);
    });
  });

  describe("isValidAmount", () => {
    it("should return true for amount within range", () => {
      expect(isValidAmount(50, 0, 100)).toBe(true);
    });

    it("should return false for amount below minimum", () => {
      expect(isValidAmount(-1, 0, 100)).toBe(false);
    });

    it("should return false for amount above maximum", () => {
      expect(isValidAmount(101, 0, 100)).toBe(false);
    });

    it("should handle edge cases", () => {
      expect(isValidAmount(0, 0, 100)).toBe(true); // minimum
      expect(isValidAmount(100, 0, 100)).toBe(true); // maximum
    });
  });

  describe("calculateFiatAmount", () => {
    it("should calculate fiat amount correctly", () => {
      expect(calculateFiatAmount(100, 23000)).toBe(2300000);
    });

    it("should handle decimal rates", () => {
      expect(calculateFiatAmount(100, 23.5)).toBe(2350);
    });

    it("should handle zero amount", () => {
      expect(calculateFiatAmount(0, 23000)).toBe(0);
    });
  });

  describe("formatNumber", () => {
    beforeEach(() => {
      mockFormat.mockImplementation((value) => value.toFixed(2));
    });

    it("should format number with correct decimal places", () => {
      const result = formatNumber(1000.567);
      expect(mockNumberFormat).toHaveBeenCalledWith("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 8,
      });
      expect(result).toBe("1000.57");
    });

    it("should handle NaN", () => {
      const result = formatNumber(NaN);
      expect(result).toBe("0");
    });

    it("should handle whole numbers", () => {
      mockFormat.mockImplementation((value) => value.toString());
      const result = formatNumber(1000);
      expect(result).toBe("1000");
    });
  });
});
