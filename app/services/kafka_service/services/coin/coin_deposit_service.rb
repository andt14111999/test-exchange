# frozen_string_literal: true

module KafkaService
  module Services
    module Coin
      class CoinDepositService < KafkaService::Base::Service
        def create(user_id:, coin:, account_key:, deposit:, amount:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_deposit_identifier(
            deposit_id: deposit.id
          )

          send_event(
            topic: KafkaService::Config::Topics::COIN_DEPOSIT,
            key: identifier,
            data: build_deposit_data(
              identifier: identifier,
              user_id: user_id,
              coin: coin,
              account_key: account_key,
              deposit: deposit,
              amount: amount
            )
          )
        end

        def create_batch(deposits, batch_size = 1000)
          @logger.info("Preparing batch deposit for #{deposits.size} deposits")

          messages = deposits.map do |deposit|
            {
              topic: KafkaService::Config::Topics::COIN_DEPOSIT,
              key: deposit[:user_id],
              payload: build_deposit_data(**deposit)
            }
          end

          producer.send_messages_batch(messages, batch_size)
        end

        private

        def generate_deposit_identifier(deposit_id:)
          "deposit-#{deposit_id}"
        end

        def build_deposit_data(identifier:, user_id:, coin:, account_key:, deposit:, amount:, **_opts)
          {
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
            actionId: deposit.id,
            userId: user_id,
            status: 'pending',
            accountKey: account_key,
            amount: amount,
            coin: coin,
            txHash: deposit.tx_hash,
            layer: deposit.coin_account.layer,
            depositAddress: deposit.coin_account.address
          }
        end
      end
    end
  end
end
