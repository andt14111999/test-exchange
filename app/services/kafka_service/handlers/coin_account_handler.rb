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
          account = find_account(payload)
          return unless account

          update_account_balance(account, payload)
        end
      rescue StandardError => e
        Rails.logger.error("Failed to update balance: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def find_account(payload)
        user_id, type, account_id = payload['key'].split('-')

        if type == 'coin'
          CoinAccount.find_by(
            id: account_id,
            user_id: user_id,
            account_type: 'main',
            layer: 'all'
          )
        else
          FiatAccount.find_by(
            id: account_id,
            user_id: user_id,
          )
        end
      end

      def update_account_balance(account, payload)
        account.update!(
          balance: BigDecimal.safe_convert(payload['totalBalance']),
          frozen_balance: BigDecimal.safe_convert(payload['frozenBalance'])
        )
      end
    end
  end
end
