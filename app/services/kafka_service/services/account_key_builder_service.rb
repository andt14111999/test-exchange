# frozen_string_literal: true

module KafkaService
  module Services
    class AccountKeyBuilderService
      def self.build_fiat_account_key(user_id:, account_id:)
        "#{user_id}-fiat-#{account_id}"
      end

      def self.build_coin_account_key(user_id:, account_id:)
        "#{user_id}-coin-#{account_id}"
      end
    end
  end
end
