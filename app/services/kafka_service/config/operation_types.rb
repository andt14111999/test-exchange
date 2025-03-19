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
    end
  end
end
