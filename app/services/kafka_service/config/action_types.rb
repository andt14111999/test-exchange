# frozen_string_literal: true

module KafkaService
  module Config
    module ActionTypes
      # Transaction types
      COIN_TRANSACTION = 'CoinTransaction'

      # Account types
      COIN_ACCOUNT = 'CoinAccount'
      FIAT_ACCOUNT = 'FiatAccount'

      # Merchant types
      MERCHANT_ESCROW = 'MerchantEscrow'

      # Trade types
      TRADE = 'Trade'

      # Offer types
      OFFER = 'Offer'
    end
  end
end
