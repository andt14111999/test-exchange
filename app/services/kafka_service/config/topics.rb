# frozen_string_literal: true

module KafkaService
  module Config
    module Topics
      # Input topics
      COIN_ACCOUNT = 'EE.I.coin_account'
      COIN_DEPOSIT = 'EE.I.coin_deposit'
      COIN_WITHDRAW = 'EE.I.coin_withdraw'
      COIN_ACCOUNT_QUERY = 'EE.I.coin_account_query'
      COIN_ACCOUNT_RESET = 'EE.I.coin_account_reset'
      MERCHANT_ESCROW = 'EE.I.merchant_escrow'
      TRADE = 'EE.I.trade'
      OFFER = 'EE.I.offer'
      BALANCES_LOCK = 'EE.I.balances_lock'

      # Output topics
      COIN_ACCOUNT_UPDATE = 'EE.O.coin_account_update'
      COIN_WITHDRAWAL_UPDATE = 'EE.O.coin_withdrawal_update'
      TRANSACTION_RESPONSE = 'EE.O.transaction_response'
      MERCHANT_ESCROW_UPDATE = 'EE.O.merchant_escrow_update'
      TRADE_UPDATE = 'EE.O.trade_update'
      OFFER_UPDATE = 'EE.O.offer_update'
      BALANCES_LOCK_UPDATE = 'EE.O.balances_lock_update'

      # Aliases
      BALANCE_UPDATE = COIN_ACCOUNT_UPDATE
      TRANSACTION_RESULT = TRANSACTION_RESPONSE
      BALANCE_RESPONSE = COIN_ACCOUNT_UPDATE

      # AMM Pool related topics
      AMM_POOL_TOPIC = 'EE.I.amm_pool'.freeze
      AMM_POOL_UPDATE_TOPIC = 'EE.O.amm_pool_update'.freeze

      # AMM Position related topics
      AMM_POSITION_TOPIC = 'EE.I.amm_position'.freeze
      AMM_POSITION_UPDATE_TOPIC = 'EE.O.amm_position_update'.freeze

      # AMM Order related topics
      AMM_ORDER_TOPIC = 'EE.I.amm_order'.freeze
      AMM_ORDER_UPDATE_TOPIC = 'EE.O.amm_order_update'.freeze

      # AMM Tick related topics
      TICK_QUERY_TOPIC = 'EE.I.tick_query'.freeze
      TICK_UPDATE_TOPIC = 'EE.O.tick_update'.freeze
    end
  end
end
