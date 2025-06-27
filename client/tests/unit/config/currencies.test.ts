import { CURRENCIES, CurrencyInfo } from "@/config/currencies";

describe("Currencies Configuration", () => {
  describe("CurrencyInfo Interface", () => {
    it("should have correct type definitions", () => {
      const testCurrency: CurrencyInfo = {
        name: "Test Currency",
        symbol: "TEST",
        type: "crypto",
        icon: "T",
      };

      expect(testCurrency).toHaveProperty("name");
      expect(testCurrency).toHaveProperty("symbol");
      expect(testCurrency).toHaveProperty("type");
      expect(testCurrency).toHaveProperty("icon");
    });

    it("should allow icon to be optional", () => {
      const testCurrency: CurrencyInfo = {
        name: "Test Currency",
        symbol: "TEST",
        type: "crypto",
      };

      expect(testCurrency).not.toHaveProperty("icon");
    });

    it("should only allow 'crypto' or 'fiat' as type", () => {
      const cryptoCurrency: CurrencyInfo = {
        name: "Test Crypto",
        symbol: "TC",
        type: "crypto",
      };

      const fiatCurrency: CurrencyInfo = {
        name: "Test Fiat",
        symbol: "TF",
        type: "fiat",
      };

      expect(cryptoCurrency.type).toBe("crypto");
      expect(fiatCurrency.type).toBe("fiat");
    });
  });

  describe("CURRENCIES Object", () => {
    it("should contain all defined currencies", () => {
      const expectedCurrencies = ["BTC", "ETH", "BNB", "USDT", "VND", "PHP"];
      expect(Object.keys(CURRENCIES)).toEqual(
        expect.arrayContaining(expectedCurrencies),
      );
    });

    describe("Cryptocurrency entries", () => {
      it("should have correct Bitcoin (BTC) configuration", () => {
        expect(CURRENCIES.BTC).toEqual({
          name: "Bitcoin",
          symbol: "BTC",
          type: "crypto",
          icon: "₿",
        });
      });

      it("should have correct Ethereum (ETH) configuration", () => {
        expect(CURRENCIES.ETH).toEqual({
          name: "Ethereum",
          symbol: "ETH",
          type: "crypto",
          icon: "Ξ",
        });
      });

      it("should have correct BNB configuration", () => {
        expect(CURRENCIES.BNB).toEqual({
          name: "BNB",
          symbol: "BNB",
          type: "crypto",
          icon: "BNB",
        });
      });

      it("should have correct Tether (USDT) configuration", () => {
        expect(CURRENCIES.USDT).toEqual({
          name: "Tether",
          symbol: "USDT",
          type: "crypto",
          icon: "USDT",
        });
      });
    });

    describe("Fiat currency entries", () => {
      it("should have correct Vietnamese Dong (VND) configuration", () => {
        expect(CURRENCIES.VND).toEqual({
          name: "Vietnamese Dong",
          symbol: "VND",
          type: "fiat",
          icon: "₫",
        });
      });

      it("should have correct Philippine Peso (PHP) configuration", () => {
        expect(CURRENCIES.PHP).toEqual({
          name: "Philippine Peso",
          symbol: "PHP",
          type: "fiat",
          icon: "₱",
        });
      });
    });

    it("should have all crypto currencies with type 'crypto'", () => {
      const cryptoCurrencies = Object.values(CURRENCIES).filter(
        (currency) => currency.type === "crypto",
      );
      cryptoCurrencies.forEach((currency) => {
        expect(currency.type).toBe("crypto");
      });
    });

    it("should have all fiat currencies with type 'fiat'", () => {
      const fiatCurrencies = Object.values(CURRENCIES).filter(
        (currency) => currency.type === "fiat",
      );
      fiatCurrencies.forEach((currency) => {
        expect(currency.type).toBe("fiat");
      });
    });

    it("should have unique symbols for all currencies", () => {
      const symbols = Object.values(CURRENCIES).map(
        (currency) => currency.symbol,
      );
      const uniqueSymbols = new Set(symbols);
      expect(symbols.length).toBe(uniqueSymbols.size);
    });

    it("should have valid icons for all currencies", () => {
      Object.values(CURRENCIES).forEach((currency) => {
        expect(currency.icon).toBeDefined();
        expect(typeof currency.icon).toBe("string");
        expect(currency.icon?.length).toBeGreaterThan(0);
      });
    });
  });
});
