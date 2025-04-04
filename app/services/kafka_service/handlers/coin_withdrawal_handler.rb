# frozen_string_literal: true

module KafkaService
  module Handlers
    class CoinWithdrawalHandler < BaseHandler
      def handle(payload)
        case payload['operationType']
        when Config::OperationTypes::COIN_WITHDRAWAL_CREATE
          process_withdrawal_create(payload)
        when Config::OperationTypes::COIN_WITHDRAWAL_RELEASING
          process_withdrawal_releasing(payload)
        when Config::OperationTypes::COIN_WITHDRAWAL_FAILED
          process_withdrawal_failed(payload)
        end
      end

      private

      def process_withdrawal_create(payload)
        Rails.logger.info("Processing withdrawal create: #{payload['identifier']}")
        # Implement withdrawal creation logic
      end

      def process_withdrawal_releasing(payload)
        Rails.logger.info("Processing withdrawal releasing: #{payload['identifier']}")
        # Implement withdrawal releasing logic
      end

      def process_withdrawal_failed(payload)
        Rails.logger.info("Processing withdrawal failed: #{payload['identifier']}")
        # Implement withdrawal failure logic
      end
    end
  end
end
