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

        account = CoinAccount.find_or_initialize_by(
          user_id: payload['userId'],
          coin_type: payload['coin'].upcase,
          account_type: 'main',
          layer: 'all'
        )

        account.update!(
          balance: payload['totalBalance'],
          frozen_balance: payload['frozenBalance']
        )
      end
    end
  end
end
