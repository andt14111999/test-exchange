# frozen_string_literal: true

module KafkaService
  module Services
    module Fiat
      class FiatAccountService < KafkaService::Base::Service
        def create(user_id:, currency:, account_id: nil)
          account_key = KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
            user_id: user_id,
            account_id: account_id
          )

          send_event(
            topic: KafkaService::Config::Topics::COIN_ACCOUNT,
            key: account_key,
            data: {
              operationType: KafkaService::Config::OperationTypes::COIN_ACCOUNT_CREATE,
              actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
              actionId: account_id,
              userId: user_id,
              coin: currency,
              accountKey: account_key
            }
          )
        end

        def query_balance(user_id:, account_id:)
          account_key = KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
            user_id: user_id,
            account_id: account_id
          )

          send_event(
            topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
            key: account_key,
            data: {
              actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
              actionId: account_id,
              operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY,
              accountKey: account_key
            }
          )
        end

        def reset_balance(user_id:, account_id:)
          account_key = KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
            user_id: user_id,
            account_id: account_id
          )

          send_event(
            topic: KafkaService::Config::Topics::COIN_ACCOUNT_RESET,
            key: account_key,
            data: {
              accountKey: account_key
            }
          )
        end
      end
    end
  end
end
