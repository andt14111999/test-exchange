# frozen_string_literal: true

module KafkaService
  module Services
    module Coin
      class CoinWithdrawalService < KafkaService::Base::Service
        def create(identifier:, status:, user_id:, coin:, account_key:, amount:, fee:, recipient_account_key: nil)
          data = {
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
            actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
            actionId: SecureRandom.uuid,
            userId: user_id,
            status: status,
            accountKey: account_key,
            amount: amount,
            coin: coin,
            txHash: "tx-#{SecureRandom.hex(16)}",
            layer: 'L1',
            destinationAddress: "address-#{SecureRandom.hex(16)}",
            fee: fee
          }

          # Include recipient_account_key for internal transfers if available
          data[:recipientAccountKey] = recipient_account_key if recipient_account_key.present?

          send_event(
            topic: KafkaService::Config::Topics::COIN_WITHDRAW,
            key: identifier,
            data: data
          )
        end

        def update_status(identifier:, operation_type:)
          send_event(
            topic: KafkaService::Config::Topics::COIN_WITHDRAW,
            key: identifier,
            data: {
              operationType: operation_type,
              actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
              actionId: SecureRandom.uuid,
              identifier: identifier
            }
          )
        end
      end
    end
  end
end
