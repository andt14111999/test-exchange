# frozen_string_literal: true

module KafkaService
  module Services
    module Coin
      class BalanceLockService < KafkaService::Base::Service
        def create(account_keys:, identifier:, lock_id: nil)
          data = {
            accountKeys: account_keys,
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::BALANCES_LOCK_CREATE,
            actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
            actionId: SecureRandom.uuid
          }

          data[:lockId] = lock_id if lock_id

          send_event(
            topic: KafkaService::Config::Topics::BALANCES_LOCK,
            key: identifier,
            data: data
          )
        end

        def unlock(lock_id:, identifier:)
          data = {
            lockId: lock_id,
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::BALANCES_LOCK_RELEASE,
            actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
            actionId: SecureRandom.uuid
          }

          send_event(
            topic: KafkaService::Config::Topics::BALANCES_LOCK,
            key: identifier,
            data: data
          )
        end
      end
    end
  end
end
