import { API_BASE_URL, WS_URL, API_ENDPOINTS } from "@/lib/api/config";

describe("API Configuration", () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  describe("Base URLs", () => {
    it("should use default API_BASE_URL when environment variable is not set", () => {
      delete process.env.NEXT_PUBLIC_API_BASE_URL;
      expect(API_BASE_URL).toBe("http://localhost:3969/api/v1");
    });

    it("should use default WS_URL when environment variable is not set", () => {
      delete process.env.NEXT_PUBLIC_WS_URL;
      expect(WS_URL).toBe("ws://localhost:3969");
    });

    it("should use environment variable for API_BASE_URL when set", async () => {
      process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.example.com";
      // Need to re-import to get updated env values
      const { API_BASE_URL: updatedApiBaseUrl } = await import(
        "@/lib/api/config"
      );
      expect(updatedApiBaseUrl).toBe("https://api.example.com");
    });

    it("should use environment variable for WS_URL when set", async () => {
      process.env.NEXT_PUBLIC_WS_URL = "wss://ws.example.com";
      // Need to re-import to get updated env values
      const { WS_URL: updatedWsUrl } = await import("@/lib/api/config");
      expect(updatedWsUrl).toBe("wss://ws.example.com");
    });
  });

  describe("API_ENDPOINTS", () => {
    describe("auth endpoints", () => {
      it("should have correct google auth endpoint", () => {
        expect(API_ENDPOINTS.auth.google).toBe("/auth/google");
      });
    });

    describe("users endpoints", () => {
      it("should have correct me endpoint", () => {
        expect(API_ENDPOINTS.users.me).toBe("/users/me");
      });
    });

    describe("wallet endpoints", () => {
      it("should have correct balances endpoint", () => {
        expect(API_ENDPOINTS.wallet.balances).toBe("/balances");
      });

      it("should have correct transactions endpoint", () => {
        expect(API_ENDPOINTS.wallet.transactions).toBe("/coin_transactions");
      });
    });

    describe("fiat endpoints", () => {
      it("should have correct deposits and withdrawals endpoints", () => {
        expect(API_ENDPOINTS.fiat.deposits).toBe("/fiat_deposits");
        expect(API_ENDPOINTS.fiat.withdrawals).toBe("/fiat_withdrawals");
      });

      describe("deposit operations", () => {
        it("should generate correct deposit endpoints", () => {
          const depositId = "123";
          expect(API_ENDPOINTS.fiat.deposit.create).toBe("/fiat_deposits");
          expect(API_ENDPOINTS.fiat.deposit.createForTrade).toBe(
            "/fiat_deposits/for_trade",
          );
          expect(API_ENDPOINTS.fiat.deposit.get(depositId)).toBe(
            "/fiat_deposits/123",
          );
          expect(API_ENDPOINTS.fiat.deposit.markMoneySent(depositId)).toBe(
            "/fiat_deposits/123/money_sent",
          );
          expect(API_ENDPOINTS.fiat.deposit.verifyOwnership(depositId)).toBe(
            "/fiat_deposits/123/verify_ownership",
          );
          expect(API_ENDPOINTS.fiat.deposit.cancel(depositId)).toBe(
            "/fiat_deposits/123/cancel",
          );
        });
      });

      describe("withdrawal operations", () => {
        it("should generate correct withdrawal endpoints", () => {
          const withdrawalId = "456";
          expect(API_ENDPOINTS.fiat.withdrawal.create).toBe(
            "/fiat_withdrawals",
          );
          expect(API_ENDPOINTS.fiat.withdrawal.get(withdrawalId)).toBe(
            "/fiat_withdrawals/456",
          );
          expect(
            API_ENDPOINTS.fiat.withdrawal.markMoneySent(withdrawalId),
          ).toBe("/fiat_withdrawals/456/money_sent");
          expect(
            API_ENDPOINTS.fiat.withdrawal.verifyOwnership(withdrawalId),
          ).toBe("/fiat_withdrawals/456/verify_ownership");
          expect(API_ENDPOINTS.fiat.withdrawal.cancel(withdrawalId)).toBe(
            "/fiat_withdrawals/456/cancel",
          );
        });
      });
    });

    describe("transactions endpoints", () => {
      it("should have correct transactions endpoints", () => {
        const transactionId = "789";
        expect(API_ENDPOINTS.transactions.list).toBe("/transactions");
        expect(API_ENDPOINTS.transactions.get(transactionId)).toBe(
          "/transactions/789",
        );
      });
    });

    describe("coinAccounts endpoints", () => {
      it("should generate correct coin account endpoints", () => {
        const coinCurrency = "BTC";
        const layer = "mainnet";
        expect(API_ENDPOINTS.coinAccounts.address(coinCurrency, layer)).toBe(
          "/coin_accounts/address?coin_currency=BTC&layer=mainnet",
        );
        expect(
          API_ENDPOINTS.coinAccounts.generateAddress(coinCurrency, layer),
        ).toBe(
          "/coin_accounts/generate_address?coin_currency=BTC&layer=mainnet",
        );
      });
    });

    describe("merchant endpoints", () => {
      it("should have correct merchant base and register endpoint", () => {
        expect(API_ENDPOINTS.merchant.base).toBe("/merchant");
        expect(API_ENDPOINTS.merchant.register).toBe("/merchant/register");
      });

      describe("mint_fiat operations", () => {
        it("should generate correct mint_fiat endpoints", () => {
          const fiatMintId = 123;
          expect(API_ENDPOINTS.merchant.mint_fiat.list).toBe(
            "/merchant/mint_fiat",
          );
          expect(API_ENDPOINTS.merchant.mint_fiat.create).toBe(
            "/merchant/mint_fiat",
          );
          expect(API_ENDPOINTS.merchant.mint_fiat.get(fiatMintId)).toBe(
            "/merchant/mint_fiat/123",
          );
          expect(API_ENDPOINTS.merchant.mint_fiat.cancel(fiatMintId)).toBe(
            "/merchant/mint_fiat/123/cancel",
          );
        });
      });

      describe("offer operations", () => {
        it("should generate correct offer endpoints", () => {
          const offerId = 456;
          expect(API_ENDPOINTS.merchant.offers.list).toBe("/offers");
          expect(API_ENDPOINTS.merchant.offers.merchantList).toBe(
            "/offers/merchant",
          );
          expect(API_ENDPOINTS.merchant.offers.create).toBe("/offers");
          expect(API_ENDPOINTS.merchant.offers.get(offerId)).toBe(
            "/offers/456",
          );
          expect(API_ENDPOINTS.merchant.offers.update(offerId)).toBe(
            "/offers/456",
          );
          expect(API_ENDPOINTS.merchant.offers.delete(offerId)).toBe(
            "/offers/456",
          );
          expect(API_ENDPOINTS.merchant.offers.enable(offerId)).toBe(
            "/offers/456/enable",
          );
          expect(API_ENDPOINTS.merchant.offers.disable(offerId)).toBe(
            "/offers/456/disable",
          );
          expect(API_ENDPOINTS.merchant.offers.setOnlineStatus(offerId)).toBe(
            "/offers/456/online_status",
          );
        });
      });
    });

    describe("trades endpoints", () => {
      it("should generate correct trade endpoints", () => {
        const tradeId = "trade123";
        expect(API_ENDPOINTS.trades.list).toBe("/trades");
        expect(API_ENDPOINTS.trades.create).toBe("/trades");
        expect(API_ENDPOINTS.trades.get(tradeId)).toBe("/trades/trade123");
        expect(API_ENDPOINTS.trades.markPaid(tradeId)).toBe(
          "/trades/trade123/mark_paid",
        );
        expect(API_ENDPOINTS.trades.release(tradeId)).toBe(
          "/trades/trade123/complete",
        );
        expect(API_ENDPOINTS.trades.dispute(tradeId)).toBe(
          "/trades/trade123/dispute",
        );
        expect(API_ENDPOINTS.trades.cancel(tradeId)).toBe(
          "/trades/trade123/cancel",
        );
      });

      describe("fiat deposit operations", () => {
        it("should generate correct fiat deposit endpoints for trades", () => {
          const tradeId = "trade456";
          expect(API_ENDPOINTS.trades.fiatDeposit.create(tradeId)).toBe(
            "/trades/trade456/fiat_deposit",
          );
          expect(API_ENDPOINTS.trades.fiatDeposit.get(tradeId)).toBe(
            "/trades/trade456/fiat_deposit",
          );
          expect(API_ENDPOINTS.trades.fiatDeposit.markMoneySent(tradeId)).toBe(
            "/trades/trade456/fiat_deposit/money_sent",
          );
          expect(
            API_ENDPOINTS.trades.fiatDeposit.verifyOwnership(tradeId),
          ).toBe("/trades/trade456/fiat_deposit/verify_ownership");
          expect(API_ENDPOINTS.trades.fiatDeposit.cancel(tradeId)).toBe(
            "/trades/trade456/fiat_deposit/cancel",
          );
        });
      });
    });

    describe("amm endpoints", () => {
      it("should generate correct AMM endpoints", () => {
        const pair = "BTC-USDT";
        const positionId = 789;
        expect(API_ENDPOINTS.amm.pools).toBe("/amm_pools");
        expect(API_ENDPOINTS.amm.activePools).toBe("/amm_pools/active");
        expect(API_ENDPOINTS.amm.poolDetail(pair)).toBe("/amm_pools/BTC-USDT");
        expect(API_ENDPOINTS.amm.positions).toBe("/amm_positions");
        expect(API_ENDPOINTS.amm.positionDetail(positionId)).toBe(
          "/amm_positions/789",
        );
        expect(API_ENDPOINTS.amm.collectFee(positionId)).toBe(
          "/amm_positions/789/collect_fee",
        );
        expect(API_ENDPOINTS.amm.closePosition(positionId)).toBe(
          "/amm_positions/789/close",
        );
        expect(API_ENDPOINTS.amm.orders).toBe("/amm_orders");
        expect(API_ENDPOINTS.amm.ticks).toBe("/ticks");
      });
    });

    describe("withdrawals endpoints", () => {
      it("should generate correct withdrawal endpoints", () => {
        const withdrawalId = "w123";
        expect(API_ENDPOINTS.withdrawals.create).toBe("/coin_withdrawals");
        expect(API_ENDPOINTS.withdrawals.get(withdrawalId)).toBe(
          "/coin_withdrawals/w123",
        );
      });
    });

    describe("coins endpoint", () => {
      it("should have correct coins endpoint", () => {
        expect(API_ENDPOINTS.coins).toBe("/coins");
      });
    });

    describe("bank accounts endpoints", () => {
      it("should generate correct bank account endpoints", () => {
        const bankAccountId = "bank123";
        expect(API_ENDPOINTS.bankAccounts.list).toBe("/bank_accounts");
        expect(API_ENDPOINTS.bankAccounts.create).toBe("/bank_accounts");
        expect(API_ENDPOINTS.bankAccounts.update(bankAccountId)).toBe(
          "/bank_accounts/bank123",
        );
        expect(API_ENDPOINTS.bankAccounts.delete(bankAccountId)).toBe(
          "/bank_accounts/bank123",
        );
        expect(API_ENDPOINTS.bankAccounts.setPrimary(bankAccountId)).toBe(
          "/bank_accounts/bank123/set_primary",
        );
      });
    });

    describe("payment methods endpoints", () => {
      it("should generate correct payment method endpoints", () => {
        const paymentMethodId = 999;
        expect(API_ENDPOINTS.paymentMethods.list).toBe("/payment_methods");
        expect(API_ENDPOINTS.paymentMethods.get(paymentMethodId)).toBe(
          "/payment_methods/999",
        );
      });
    });

    describe("settings endpoints", () => {
      it("should have correct coinSettings endpoint", () => {
        expect(API_ENDPOINTS.settings.coinSettings).toBe("/coin_settings");
      });
    });
  });
});
