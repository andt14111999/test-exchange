export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:3969/api/v1";

export const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:3969";

export const API_ENDPOINTS = {
  auth: {
    google: "/auth/google",
  },
  users: {
    me: "/users/me",
    updateUsername: "/users/username",
  },
  wallet: {
    balances: "/balances",
    transactions: "/coin_transactions",
  },
  fiat: {
    deposits: "/fiat_deposits",
    withdrawals: "/fiat_withdrawals",
    deposit: {
      create: "/fiat_deposits",
      createForTrade: "/fiat_deposits/for_trade",
      get: (id: string) => `/fiat_deposits/${id}`,
      markMoneySent: (id: string) => `/fiat_deposits/${id}/money_sent`,
      verifyOwnership: (id: string) => `/fiat_deposits/${id}/verify_ownership`,
      cancel: (id: string) => `/fiat_deposits/${id}/cancel`,
    },
    withdrawal: {
      create: "/fiat_withdrawals",
      get: (id: string) => `/fiat_withdrawals/${id}`,
      markMoneySent: (id: string) => `/fiat_withdrawals/${id}/money_sent`,
      verifyOwnership: (id: string) =>
        `/fiat_withdrawals/${id}/verify_ownership`,
      cancel: (id: string) => `/fiat_withdrawals/${id}/cancel`,
    },
  },
  transactions: {
    list: "/transactions",
    get: (id: string) => `/transactions/${id}`,
  },
  coinAccounts: {
    address: (coinCurrency: string, layer: string) =>
      `/coin_accounts/address?coin_currency=${coinCurrency}&layer=${layer}`,
    generateAddress: (coinCurrency: string, layer: string) =>
      `/coin_accounts/generate_address?coin_currency=${coinCurrency}&layer=${layer}`,
  },
  merchant: {
    register: "/merchant_registration",
    escrows: {
      list: "/merchant_escrows",
      create: "/merchant_escrows",
      get: (id: number) => `/merchant_escrows/${id}`,
      cancel: (id: number) => `/merchant_escrows/${id}/cancel`,
    },
    offers: {
      list: "/offers",
      merchantList: "/offers/merchant",
      get: (id: number) => `/offers/${id}`,
      create: "/offers",
      update: (id: number) => `/offers/${id}`,
      delete: (id: number) => `/offers/${id}`,
      enable: (id: number) => `/offers/${id}/enable`,
      disable: (id: number) => `/offers/${id}/disable`,
      setOnlineStatus: (id: number) => `/offers/${id}/online_status`,
    },
  },
  trades: {
    list: "/trades",
    get: (id: string) => `/trades/${id}`,
    create: "/trades",
    markPaid: (id: string) => `/trades/${id}/mark_paid`,
    release: (id: string) => `/trades/${id}/complete`,
    dispute: (id: string) => `/trades/${id}/dispute`,
    cancel: (id: string) => `/trades/${id}/cancel`,
    fiatDeposit: {
      create: (tradeId: string) => `/trades/${tradeId}/fiat_deposit`,
      get: (tradeId: string) => `/trades/${tradeId}/fiat_deposit`,
      markMoneySent: (tradeId: string) =>
        `/trades/${tradeId}/fiat_deposit/money_sent`,
      verifyOwnership: (tradeId: string) =>
        `/trades/${tradeId}/fiat_deposit/verify_ownership`,
      cancel: (tradeId: string) => `/trades/${tradeId}/fiat_deposit/cancel`,
    },
  },
  amm: {
    pools: "/amm_pools",
    activePools: "/amm_pools/active",
    poolDetail: (pair: string) => `/amm_pools/${pair}`,
    positions: "/amm_positions",
    positionDetail: (id: number) => `/amm_positions/${id}`,
    collectFee: (id: number) => `/amm_positions/${id}/collect_fee`,
    closePosition: (id: number) => `/amm_positions/${id}/close`,
    orders: "/amm_orders",
    ticks: "/ticks",
  },
  withdrawals: {
    create: "/coin_withdrawals",
    get: (id: string) => `/coin_withdrawals/${id}`,
  },
  coins: "/coins",
  banks: "/banks",
  bankAccounts: {
    list: "/bank_accounts",
    create: "/bank_accounts",
    update: (id: string) => `/bank_accounts/${id}`,
    delete: (id: string) => `/bank_accounts/${id}`,
    setPrimary: (id: string) => `/bank_accounts/${id}/set_primary`,
  },
  paymentMethods: {
    list: "/payment_methods",
    get: (id: number) => `/payment_methods/${id}`,
  },
  settings: {
    coinSettings: "/coin_settings",
  },
} as const;
