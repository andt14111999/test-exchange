# frozen_string_literal: true

module KafkaService
  module Handlers
    class CoinDepositHandler < BaseHandler
      def handle(payload)
        case payload['operationType']
        when Config::OperationType::COIN_DEPOSIT_CREATE
          process_deposit_create(payload)
        end
      end

      private

      def process_deposit_create(payload)
        Rails.logger.info("Processing deposit create: #{payload['identifier']}")
        # Implement deposit creation logic
      end
    end
  end
end
