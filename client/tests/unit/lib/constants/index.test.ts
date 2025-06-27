import {
  NETWORKS,
  FIAT_CURRENCIES,
  TRADE_EXPIRY_MINUTES,
  FIAT_CURRENCY_SYMBOLS,
  NETWORK_LABELS,
  MOCK_RATES,
  MOCK_AMOUNT_LIMITS,
  DEPOSIT_FEES,
} from "@/lib/constants";

describe("Constants", () => {
  describe("NETWORKS", () => {
    it("should contain the correct network types", () => {
      expect(NETWORKS).toEqual(["ERC20", "TRC20", "BEP20"]);
      expect(NETWORKS).toHaveLength(3);
    });
  });

  describe("FIAT_CURRENCIES", () => {
    it("should contain the correct fiat currencies", () => {
      expect(FIAT_CURRENCIES).toEqual(["VND", "PHP", "NGN"]);
      expect(FIAT_CURRENCIES).toHaveLength(3);
    });
  });

  describe("TRADE_EXPIRY_MINUTES", () => {
    it("should have the correct expiry time", () => {
      expect(TRADE_EXPIRY_MINUTES).toBe(30);
    });
  });

  describe("FIAT_CURRENCY_SYMBOLS", () => {
    it("should have the correct currency symbols", () => {
      expect(FIAT_CURRENCY_SYMBOLS).toEqual({
        VND: "₫",
        PHP: "₱",
      });
      expect(Object.keys(FIAT_CURRENCY_SYMBOLS)).toHaveLength(2);
    });

    it("should have the correct symbol for each currency", () => {
      expect(FIAT_CURRENCY_SYMBOLS.VND).toBe("₫");
      expect(FIAT_CURRENCY_SYMBOLS.PHP).toBe("₱");
    });
  });

  describe("NETWORK_LABELS", () => {
    it("should have the correct network labels", () => {
      expect(NETWORK_LABELS).toEqual({
        ERC20: "Ethereum (ERC20)",
        TRC20: "Tron (TRC20)",
        BEP20: "BNB Smart Chain (BEP20)",
      });
      expect(Object.keys(NETWORK_LABELS)).toHaveLength(3);
    });

    it("should have the correct label for each network", () => {
      expect(NETWORK_LABELS.ERC20).toBe("Ethereum (ERC20)");
      expect(NETWORK_LABELS.TRC20).toBe("Tron (TRC20)");
      expect(NETWORK_LABELS.BEP20).toBe("BNB Smart Chain (BEP20)");
    });
  });

  describe("MOCK_RATES", () => {
    it("should have the correct mock rates structure", () => {
      expect(MOCK_RATES).toEqual({
        VND: {
          BUY: 23500,
          SELL: 24000,
        },
        PHP: {
          BUY: 55.5,
          SELL: 56.5,
        },
      });
    });

    it("should have the correct rates for each currency", () => {
      expect(MOCK_RATES.VND.BUY).toBe(23500);
      expect(MOCK_RATES.VND.SELL).toBe(24000);
      expect(MOCK_RATES.PHP.BUY).toBe(55.5);
      expect(MOCK_RATES.PHP.SELL).toBe(56.5);
    });
  });

  describe("MOCK_AMOUNT_LIMITS", () => {
    it("should have the correct amount limits", () => {
      expect(MOCK_AMOUNT_LIMITS).toEqual({
        MIN: 50000,
        MAX: 1000000000,
      });
    });

    it("should have valid min and max values", () => {
      expect(MOCK_AMOUNT_LIMITS.MIN).toBe(50000);
      expect(MOCK_AMOUNT_LIMITS.MAX).toBe(1000000000);
      expect(MOCK_AMOUNT_LIMITS.MIN).toBeLessThan(MOCK_AMOUNT_LIMITS.MAX);
    });
  });

  describe("DEPOSIT_FEES", () => {
    it("should have the correct fee structure", () => {
      expect(DEPOSIT_FEES).toEqual({
        PERCENTAGE: 0.1,
        FIXED: {
          VND: 5000,
          PHP: 10,
          NGN: 200,
        },
      });
    });

    it("should have the correct percentage fee", () => {
      expect(DEPOSIT_FEES.PERCENTAGE).toBe(0.1);
    });

    it("should have the correct fixed fees for each currency", () => {
      expect(DEPOSIT_FEES.FIXED.VND).toBe(5000);
      expect(DEPOSIT_FEES.FIXED.PHP).toBe(10);
      expect(DEPOSIT_FEES.FIXED.NGN).toBe(200);
    });
  });
});
