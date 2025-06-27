import {
  MOCK_USERS,
  MOCK_OFFERS,
  MOCK_TRADES,
  MOCK_PAYMENT_METHODS,
} from "@/lib/constants/mock-data";

describe("Mock Data Constants", () => {
  describe("MOCK_USERS", () => {
    it("should contain the correct number of users", () => {
      expect(MOCK_USERS).toHaveLength(2);
    });

    it("should have the correct merchant user data", () => {
      const merchant = MOCK_USERS[0];
      expect(merchant).toEqual({
        id: "merchant1",
        email: "merchant1@example.com",
        role: "MERCHANT",
        name: "John Merchant",
        createdAt: new Date("2024-01-01"),
        walletAddress: "0x1234567890abcdef1234567890abcdef12345678",
      });
    });

    it("should have the correct customer user data", () => {
      const customer = MOCK_USERS[1];
      expect(customer).toEqual({
        id: "customer1",
        email: "customer1@example.com",
        role: "CUSTOMER",
        name: "Alice Customer",
        createdAt: new Date("2024-01-02"),
        walletAddress: "0xabcdef1234567890abcdef1234567890abcdef12",
      });
    });
  });

  describe("MOCK_OFFERS", () => {
    it("should contain the correct number of offers", () => {
      expect(MOCK_OFFERS).toHaveLength(6);
    });

    it("should have the correct structure for each offer", () => {
      MOCK_OFFERS.forEach((offer) => {
        expect(offer).toHaveProperty("id");
        expect(offer).toHaveProperty("type");
        expect(offer).toHaveProperty("fiatCurrency");
        expect(offer).toHaveProperty("amount");
        expect(offer).toHaveProperty("minAmount");
        expect(offer).toHaveProperty("maxAmount");
        expect(offer).toHaveProperty("isActive");
      });
    });

    it("should have valid offer types", () => {
      const types = MOCK_OFFERS.map((offer) => offer.type);
      expect(types).toContain("BUY");
      expect(types).toContain("SELL");
    });

    it("should have valid fiat currencies", () => {
      const currencies = MOCK_OFFERS.map((offer) => offer.fiatCurrency);
      expect(currencies).toContain("VND");
      expect(currencies).toContain("PHP");
      expect(currencies).toContain("NGN");
    });

    it("should have valid amount ranges", () => {
      MOCK_OFFERS.forEach((offer) => {
        expect(offer.minAmount).toBeLessThanOrEqual(offer.maxAmount);
        expect(offer.amount).toBeLessThanOrEqual(offer.maxAmount);
        expect(offer.amount).toBeGreaterThanOrEqual(offer.minAmount);
      });
    });
  });

  describe("MOCK_TRADES", () => {
    it("should contain the correct number of trades", () => {
      expect(MOCK_TRADES).toHaveLength(8);
    });

    it("should have the correct structure for each trade", () => {
      MOCK_TRADES.forEach((trade) => {
        expect(trade).toHaveProperty("id");
        expect(trade).toHaveProperty("offerId");
        expect(trade).toHaveProperty("merchantId");
        expect(trade).toHaveProperty("customerId");
        expect(trade).toHaveProperty("amount");
        expect(trade).toHaveProperty("status");
        expect(trade).toHaveProperty("type");
        expect(trade).toHaveProperty("network");
        expect(trade).toHaveProperty("fiatCurrency");
        expect(trade).toHaveProperty("rate");
        expect(trade).toHaveProperty("escrowAddress");
        expect(trade).toHaveProperty("createdAt");
        expect(trade).toHaveProperty("updatedAt");
        expect(trade).toHaveProperty("expiresAt");
      });
    });

    it("should have valid trade statuses", () => {
      const statuses = MOCK_TRADES.map((trade) => trade.status);
      expect(statuses).toContain("PENDING");
      expect(statuses).toContain("COMPLETED");
      expect(statuses).toContain("CANCELLED");
    });

    it("should have valid networks", () => {
      const networks = MOCK_TRADES.map((trade) => trade.network);
      expect(networks).toContain("ERC20");
      expect(networks).toContain("TRC20");
      expect(networks).toContain("BEP20");
    });

    it("should have valid dates", () => {
      MOCK_TRADES.forEach((trade) => {
        expect(trade.createdAt).toBeInstanceOf(Date);
        expect(trade.updatedAt).toBeInstanceOf(Date);
        expect(trade.expiresAt).toBeInstanceOf(Date);
      });
    });

    it("should have valid escrow addresses", () => {
      MOCK_TRADES.forEach((trade) => {
        expect(trade.escrowAddress).toMatch(/^0xMockEscrowAddress\d+$/);
      });
    });
  });

  describe("MOCK_PAYMENT_METHODS", () => {
    it("should contain the correct number of payment methods", () => {
      expect(MOCK_PAYMENT_METHODS).toHaveLength(2);
    });

    it("should have the correct bank payment method", () => {
      const bankPayment = MOCK_PAYMENT_METHODS[0];
      expect(bankPayment).toEqual({
        id: "payment1",
        userId: "merchant1",
        type: "BANK",
        accountNumber: "1234567890",
        accountName: "John Merchant",
        bankName: "Example Bank",
        isActive: true,
      });
    });

    it("should have the correct e-wallet payment method", () => {
      const eWalletPayment = MOCK_PAYMENT_METHODS[1];
      expect(eWalletPayment).toEqual({
        id: "payment2",
        userId: "customer1",
        type: "E_WALLET",
        accountNumber: "0987654321",
        accountName: "Alice Customer",
        isActive: true,
      });
    });

    it("should have valid payment method types", () => {
      const types = MOCK_PAYMENT_METHODS.map((method) => method.type);
      expect(types).toContain("BANK");
      expect(types).toContain("E_WALLET");
    });
  });
});
