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

      # Output topics
      COIN_ACCOUNT_UPDATE = 'EE.O.coin_account_update'
      TRANSACTION_RESPONSE = 'EE.O.transaction_response'

      # Aliases
      BALANCE_UPDATE = COIN_ACCOUNT_UPDATE
      TRANSACTION_RESULT = TRANSACTION_RESPONSE
      BALANCE_RESPONSE = COIN_ACCOUNT_UPDATE
    end
  end
end
