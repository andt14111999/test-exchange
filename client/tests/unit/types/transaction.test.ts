import {
  TransactionType,
  TransactionStatus,
  BaseTransaction,
  CryptoTransaction,
  FiatTransaction,
  TradeTransaction,
  Transaction,
  isCryptoTransaction,
  isFiatTransaction,
  isTradeTransaction,
  TRANSACTION_STATUS,
  TRANSACTION_TYPE,
} from "@/types/transaction";

describe("Transaction Types", () => {
  describe("Type Definitions", () => {
    it("should have correct TransactionType values", () => {
      const validTypes: TransactionType[] = [
        "deposit",
        "withdrawal",
        "fiat_deposit",
        "fiat_withdrawal",
        "buy",
        "sell",
      ];

      validTypes.forEach((type) => {
        // TypeScript will error if any of these assignments are invalid
        const transactionType: TransactionType = type;
        expect(transactionType).toBe(type);
      });
    });

    it("should have correct TransactionStatus values", () => {
      const validStatuses: TransactionStatus[] = [
        "pending",
        "completed",
        "failed",
        "cancelled",
        "processing",
        "verified",
      ];

      validStatuses.forEach((status) => {
        // TypeScript will error if any of these assignments are invalid
        const transactionStatus: TransactionStatus = status;
        expect(transactionStatus).toBe(status);
      });
    });
  });

  describe("Transaction Interfaces", () => {
    const baseTransaction: BaseTransaction = {
      id: "123",
      type: "deposit",
      amount: 100,
      status: "pending",
      created_at: "2024-03-20T00:00:00Z",
      updated_at: "2024-03-20T00:00:00Z",
      reference: "ref123",
    };

    it("should validate CryptoTransaction structure", () => {
      const cryptoTx: CryptoTransaction = {
        ...baseTransaction,
        type: "deposit",
        coin_currency: "BTC",
        hash: "0x123",
        address: "0xabc",
      };

      expect(cryptoTx).toHaveProperty("coin_currency");
      expect(cryptoTx).toHaveProperty("hash");
      expect(cryptoTx).toHaveProperty("address");
      expect(cryptoTx.type).toMatch(/^(deposit|withdrawal)$/);
    });

    it("should validate FiatTransaction structure", () => {
      const fiatTx: FiatTransaction = {
        ...baseTransaction,
        type: "fiat_deposit",
        currency: "USD",
        bank_account_id: "bank123",
      };

      expect(fiatTx).toHaveProperty("currency");
      expect(fiatTx).toHaveProperty("bank_account_id");
      expect(fiatTx.type).toMatch(/^(fiat_deposit|fiat_withdrawal)$/);
    });

    it("should validate TradeTransaction structure", () => {
      const tradeTx: TradeTransaction = {
        ...baseTransaction,
        type: "buy",
        coin_currency: "BTC",
        fiat_currency: "USD",
        price: 50000,
      };

      expect(tradeTx).toHaveProperty("coin_currency");
      expect(tradeTx).toHaveProperty("fiat_currency");
      expect(tradeTx).toHaveProperty("price");
      expect(tradeTx.type).toMatch(/^(buy|sell)$/);
    });
  });

  describe("Type Guards", () => {
    const baseTransaction: BaseTransaction = {
      id: "123",
      type: "deposit",
      amount: 100,
      status: "pending",
      created_at: "2024-03-20T00:00:00Z",
      updated_at: "2024-03-20T00:00:00Z",
    };

    describe("isCryptoTransaction", () => {
      it("should return true for deposit transactions", () => {
        const tx: Transaction = {
          ...baseTransaction,
          type: "deposit",
          coin_currency: "BTC",
          hash: "0x123",
        };
        expect(isCryptoTransaction(tx)).toBe(true);
      });

      it("should return true for withdrawal transactions", () => {
        const tx: Transaction = {
          ...baseTransaction,
          type: "withdrawal",
          coin_currency: "ETH",
          hash: "0x456",
        };
        expect(isCryptoTransaction(tx)).toBe(true);
      });

      it("should return false for non-crypto transactions", () => {
        const fiatTx: Transaction = {
          ...baseTransaction,
          type: "fiat_deposit",
          currency: "USD",
        };
        const tradeTx: Transaction = {
          ...baseTransaction,
          type: "buy",
          coin_currency: "BTC",
          fiat_currency: "USD",
          price: 50000,
        };
        expect(isCryptoTransaction(fiatTx)).toBe(false);
        expect(isCryptoTransaction(tradeTx)).toBe(false);
      });
    });

    describe("isFiatTransaction", () => {
      it("should return true for fiat deposit transactions", () => {
        const tx: Transaction = {
          ...baseTransaction,
          type: "fiat_deposit",
          currency: "USD",
        };
        expect(isFiatTransaction(tx)).toBe(true);
      });

      it("should return true for fiat withdrawal transactions", () => {
        const tx: Transaction = {
          ...baseTransaction,
          type: "fiat_withdrawal",
          currency: "EUR",
        };
        expect(isFiatTransaction(tx)).toBe(true);
      });

      it("should return false for non-fiat transactions", () => {
        const cryptoTx: Transaction = {
          ...baseTransaction,
          type: "deposit",
          coin_currency: "BTC",
          hash: "0x123",
        };
        const tradeTx: Transaction = {
          ...baseTransaction,
          type: "buy",
          coin_currency: "BTC",
          fiat_currency: "USD",
          price: 50000,
        };
        expect(isFiatTransaction(cryptoTx)).toBe(false);
        expect(isFiatTransaction(tradeTx)).toBe(false);
      });
    });

    describe("isTradeTransaction", () => {
      it("should return true for buy transactions", () => {
        const tx: Transaction = {
          ...baseTransaction,
          type: "buy",
          coin_currency: "BTC",
          fiat_currency: "USD",
          price: 50000,
        };
        expect(isTradeTransaction(tx)).toBe(true);
      });

      it("should return true for sell transactions", () => {
        const tx: Transaction = {
          ...baseTransaction,
          type: "sell",
          coin_currency: "ETH",
          fiat_currency: "EUR",
          price: 2000,
        };
        expect(isTradeTransaction(tx)).toBe(true);
      });

      it("should return false for non-trade transactions", () => {
        const cryptoTx: Transaction = {
          ...baseTransaction,
          type: "deposit",
          coin_currency: "BTC",
          hash: "0x123",
        };
        const fiatTx: Transaction = {
          ...baseTransaction,
          type: "fiat_deposit",
          currency: "USD",
        };
        expect(isTradeTransaction(cryptoTx)).toBe(false);
        expect(isTradeTransaction(fiatTx)).toBe(false);
      });
    });
  });

  describe("Constants", () => {
    it("should have correct TRANSACTION_STATUS values", () => {
      expect(TRANSACTION_STATUS).toEqual({
        PENDING: "pending",
        COMPLETED: "completed",
        FAILED: "failed",
        CANCELLED: "cancelled",
        PROCESSING: "processing",
        VERIFIED: "verified",
      });
    });

    it("should have correct TRANSACTION_TYPE values", () => {
      expect(TRANSACTION_TYPE).toEqual({
        DEPOSIT: "deposit",
        WITHDRAWAL: "withdrawal",
        FIAT_DEPOSIT: "fiat_deposit",
        FIAT_WITHDRAWAL: "fiat_withdrawal",
        BUY: "buy",
        SELL: "sell",
      });
    });

    it("should be readonly constants", () => {
      expect(() => {
        // @ts-expect-error Testing runtime immutability
        TRANSACTION_STATUS.PENDING = "changed";
      }).toThrow();

      expect(() => {
        // @ts-expect-error Testing runtime immutability
        TRANSACTION_TYPE.DEPOSIT = "changed";
      }).toThrow();
    });
  });
});
