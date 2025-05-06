# frozen_string_literal: true

module KafkaService
  module Services
    class IdentifierBuilderService
      # Merchant identifiers
      def self.build_merchant_escrow_identifier(escrow_id:)
        "merchant-escrow-#{escrow_id}"
      end

      # Coin identifiers
      def self.build_deposit_identifier(deposit_id:)
        "deposit-#{deposit_id}"
      end

      def self.build_withdrawal_identifier(withdrawal_id:)
        "withdrawal-#{withdrawal_id}"
      end

      # Fiat identifiers
      def self.build_fiat_deposit_identifier(deposit_id:)
        "fiat-deposit-#{deposit_id}"
      end

      def self.build_fiat_withdrawal_identifier(withdrawal_id:)
        "fiat-withdrawal-#{withdrawal_id}"
      end

      # Trade identifiers
      def self.build_trade_identifier(trade_id:)
        "trade-#{trade_id}"
      end

      # Offer identifiers
      def self.build_offer_identifier(offer_id:)
        "offer-#{offer_id}"
      end
    end
  end
end
