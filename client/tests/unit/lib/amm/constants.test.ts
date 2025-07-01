import { VND, USDT, PHP, NGN, getTokenDecimals } from "@/lib/amm/constants";

describe("AMM Constants", () => {
  describe("Token Constants", () => {
    it("should have correct VND token configuration", () => {
      const expectedVND = {
        id: "vnd",
        symbol: "₫",
        name: "Vietnamese Dong",
        decimals: 0,
      };
      expect(VND).toEqual(expectedVND);
    });

    it("should have correct USDT token configuration", () => {
      const expectedUSDT = {
        id: "usdt",
        symbol: "$",
        name: "USD Tether",
        decimals: 6,
      };
      expect(USDT).toEqual(expectedUSDT);
    });

    it("should have correct PHP token configuration", () => {
      const expectedPHP = {
        id: "php",
        symbol: "₱",
        name: "Philippine Peso",
        decimals: 2,
      };
      expect(PHP).toEqual(expectedPHP);
    });

    it("should have correct NGN token configuration", () => {
      const expectedNGN = {
        id: "ngn",
        symbol: "₦",
        name: "Nigerian Naira",
        decimals: 2,
      };
      expect(NGN).toEqual(expectedNGN);
    });
  });

  describe("Token Decimals Function", () => {
    it("should return correct decimals for token IDs", () => {
      expect(getTokenDecimals("vnd")).toBe(0);
      expect(getTokenDecimals("usdt")).toBe(6);
      expect(getTokenDecimals("php")).toBe(2);
      expect(getTokenDecimals("ngn")).toBe(2);
    });

    it("should handle case-insensitive token IDs", () => {
      expect(getTokenDecimals("VND")).toBe(0);
      expect(getTokenDecimals("USDT")).toBe(6);
      expect(getTokenDecimals("PHP")).toBe(2);
      expect(getTokenDecimals("NGN")).toBe(2);
    });

    it("should return default decimals for unknown tokens", () => {
      expect(getTokenDecimals("BTC")).toBe(6);
      expect(getTokenDecimals("ETH")).toBe(6);
      expect(getTokenDecimals("unknown")).toBe(6);
    });

    it("should handle token symbols", () => {
      expect(getTokenDecimals("₫")).toBe(0);
      expect(getTokenDecimals("$")).toBe(6);
      expect(getTokenDecimals("₱")).toBe(2);
      expect(getTokenDecimals("₦")).toBe(2);
    });
  });
});
