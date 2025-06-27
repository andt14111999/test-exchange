import { useBalanceStore } from "@/lib/store/balance-store";
import type { BalanceData } from "@/lib/api/balance";

describe("Balance Store", () => {
  const mockBalanceData: BalanceData = {
    coin_accounts: [
      {
        coin_currency: "BTC",
        balance: 1.5,
        frozen_balance: 0.5,
      },
      {
        coin_currency: "ETH",
        balance: 10,
        frozen_balance: 2,
      },
    ],
    fiat_accounts: [
      {
        currency: "USD",
        balance: 1000,
        frozen_balance: 100,
      },
      {
        currency: "EUR",
        balance: 800,
        frozen_balance: 50,
      },
    ],
  };

  beforeEach(() => {
    // Reset store to initial state before each test
    useBalanceStore.setState({
      balanceData: null,
      balanceUpdated: false,
      setBalanceData: useBalanceStore.getState().setBalanceData,
      updateCoinBalance: useBalanceStore.getState().updateCoinBalance,
      updateFiatBalance: useBalanceStore.getState().updateFiatBalance,
      setBalanceUpdated: useBalanceStore.getState().setBalanceUpdated,
    });
  });

  describe("Initial State", () => {
    it("should have null balance data", () => {
      expect(useBalanceStore.getState().balanceData).toBeNull();
    });

    it("should have balanceUpdated set to false", () => {
      expect(useBalanceStore.getState().balanceUpdated).toBe(false);
    });
  });

  describe("setBalanceData", () => {
    it("should set balance data correctly", () => {
      useBalanceStore.getState().setBalanceData(mockBalanceData);
      expect(useBalanceStore.getState().balanceData).toEqual(mockBalanceData);
    });
  });

  describe("updateCoinBalance", () => {
    beforeEach(() => {
      useBalanceStore.getState().setBalanceData(mockBalanceData);
    });

    it("should update existing coin balance", () => {
      useBalanceStore.getState().updateCoinBalance("BTC", 2.0, 0.8);
      const state = useBalanceStore.getState();

      expect(
        state.balanceData?.coin_accounts.find((a) => a.coin_currency === "BTC"),
      ).toEqual({
        coin_currency: "BTC",
        balance: 2.0,
        frozen_balance: 0.8,
      });
      expect(state.balanceUpdated).toBe(true);
    });

    it("should add new coin balance if currency doesn't exist", () => {
      useBalanceStore.getState().updateCoinBalance("DOGE", 100, 10);
      const state = useBalanceStore.getState();

      expect(
        state.balanceData?.coin_accounts.find(
          (a) => a.coin_currency === "DOGE",
        ),
      ).toEqual({
        coin_currency: "DOGE",
        balance: 100,
        frozen_balance: 10,
      });
      expect(state.balanceUpdated).toBe(true);
    });

    it("should return null balance data if no initial data exists", () => {
      useBalanceStore.setState({ balanceData: null });
      useBalanceStore.getState().updateCoinBalance("BTC", 1.0, 0.1);
      expect(useBalanceStore.getState().balanceData).toBeNull();
    });

    it("should not affect other coin balances when updating one", () => {
      useBalanceStore.getState().updateCoinBalance("BTC", 2.0, 0.8);
      const state = useBalanceStore.getState();

      expect(
        state.balanceData?.coin_accounts.find((a) => a.coin_currency === "ETH"),
      ).toEqual({
        coin_currency: "ETH",
        balance: 10,
        frozen_balance: 2,
      });
    });
  });

  describe("updateFiatBalance", () => {
    beforeEach(() => {
      useBalanceStore.getState().setBalanceData(mockBalanceData);
    });

    it("should update existing fiat balance", () => {
      useBalanceStore.getState().updateFiatBalance("USD", 1500, 200);
      const state = useBalanceStore.getState();

      expect(
        state.balanceData?.fiat_accounts.find((a) => a.currency === "USD"),
      ).toEqual({
        currency: "USD",
        balance: 1500,
        frozen_balance: 200,
      });
      expect(state.balanceUpdated).toBe(true);
    });

    it("should add new fiat balance if currency doesn't exist", () => {
      useBalanceStore.getState().updateFiatBalance("GBP", 2000, 150);
      const state = useBalanceStore.getState();

      expect(
        state.balanceData?.fiat_accounts.find((a) => a.currency === "GBP"),
      ).toEqual({
        currency: "GBP",
        balance: 2000,
        frozen_balance: 150,
      });
      expect(state.balanceUpdated).toBe(true);
    });

    it("should return null balance data if no initial data exists", () => {
      useBalanceStore.setState({ balanceData: null });
      useBalanceStore.getState().updateFiatBalance("USD", 1000, 100);
      expect(useBalanceStore.getState().balanceData).toBeNull();
    });

    it("should not affect other fiat balances when updating one", () => {
      useBalanceStore.getState().updateFiatBalance("USD", 1500, 200);
      const state = useBalanceStore.getState();

      expect(
        state.balanceData?.fiat_accounts.find((a) => a.currency === "EUR"),
      ).toEqual({
        currency: "EUR",
        balance: 800,
        frozen_balance: 50,
      });
    });
  });

  describe("setBalanceUpdated", () => {
    it("should set balanceUpdated flag", () => {
      useBalanceStore.getState().setBalanceUpdated(true);
      expect(useBalanceStore.getState().balanceUpdated).toBe(true);

      useBalanceStore.getState().setBalanceUpdated(false);
      expect(useBalanceStore.getState().balanceUpdated).toBe(false);
    });
  });
});
