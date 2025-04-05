# frozen_string_literal: true

module KafkaService
  module Config
    module OperationTypes
      COIN_DEPOSIT_CREATE = 'coin_deposit_create'
      COIN_WITHDRAWAL_CREATE = 'coin_withdrawal_create'
      COIN_WITHDRAWAL_RELEASING = 'coin_withdrawal_releasing'
      COIN_WITHDRAWAL_FAILED = 'coin_withdrawal_failed'
      COIN_ACCOUNT_CREATE = 'coin_account_create'
      BALANCE_QUERY = 'balance_query'
      AMM_POOL_CREATE = 'amm_pool_create'
      AMM_POOL_UPDATE = 'amm_pool_update'
    end
  end
end
