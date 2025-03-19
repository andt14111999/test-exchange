# frozen_string_literal: true

module KafkaService
  module Handlers
    class CoinAccountHandler < BaseHandler
      def handle(payload)
        process_balance_update(payload)
      end

      private

      def process_balance_update(payload)
        Rails.logger.info("Processing balance update: #{payload}")

        ActiveRecord::Base.transaction do
          account = find_or_create_account(payload)
          update_account_balance(account, payload)
        end
      rescue StandardError => e
        Rails.logger.error("Failed to update balance: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def find_or_create_account(payload)
        CoinAccount.find_or_initialize_by(
          user_id: payload['userId'],
          coin_currency: payload['coin'].downcase,
          account_type: 'main',
          layer: 'all'
        )
      end

      def update_account_balance(account, payload)
        account.update!(
          balance: payload['totalBalance'],
          frozen_balance: payload['frozenBalance']
        )
      end
    end
  end
end
