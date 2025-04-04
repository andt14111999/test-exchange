# frozen_string_literal: true

module KafkaService
  module Handlers
    class CoinWithdrawalHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

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
      end

      def process_withdrawal_releasing(payload)
        Rails.logger.info("Processing withdrawal releasing: #{payload['identifier']}")
      end

      def process_withdrawal_failed(payload)
        Rails.logger.info("Processing withdrawal failed: #{payload['identifier']}")
      end
    end
  end
end
