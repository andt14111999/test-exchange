# frozen_string_literal: true

module KafkaService
  module Config
    module OperationTypes
      # Coin operations
      COIN_DEPOSIT_CREATE = 'coin_deposit_create'
      COIN_WITHDRAWAL_CREATE = 'coin_withdrawal_create'
      COIN_WITHDRAWAL_RELEASING = 'coin_withdrawal_releasing'
      COIN_WITHDRAWAL_FAILED = 'coin_withdrawal_failed'
      COIN_WITHDRAWAL_CANCELLED = 'coin_withdrawal_cancelled'
      COIN_ACCOUNT_CREATE = 'coin_account_create'
      COIN_ACCOUNT_QUERY_BALANCE = 'coin_account_query_balance'
      COIN_ACCOUNT_RESET_BALANCE = 'coin_account_reset_balance'

      # AMM operations
      AMM_POOL_CREATE = 'amm_pool_create'
      AMM_POOL_UPDATE = 'amm_pool_update'

      # AMM Position operations
      AMM_POSITION_CREATE = 'amm_position_create'
      AMM_POSITION_UPDATE = 'amm_position_update'
      AMM_POSITION_COLLECT_FEE = 'amm_position_collect_fee'
      AMM_POSITION_CLOSE = 'amm_position_close'

      # Merchant operations
      MERCHANT_ESCROW_MINT = 'merchant_escrow_mint'
      MERCHANT_ESCROW_BURN = 'merchant_escrow_burn'

      # Trade operations
      TRADE_CREATE = 'trade_create'
      TRADE_UPDATE = 'trade_update'
      TRADE_CANCEL = 'trade_cancel'
      TRADE_COMPLETE = 'trade_complete'

      # Offer operations
      OFFER_CREATE = 'offer_create'
      OFFER_UPDATE = 'offer_update'
      OFFER_DISABLE = 'offer_disable'
      OFFER_ENABLE = 'offer_enable'
      OFFER_DELETE = 'offer_delete'

      # Tick operations
      TICK_QUERY = 'tick_query'

      # Legacy
      BALANCE_QUERY = 'balance_query'
    end
  end
end
