# frozen_string_literal: true

module KafkaService
  module Handlers
    class CoinDepositHandler < BaseHandler
      def handle(payload)
        case payload['object']['operationType']
        when KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE
          process_deposit_created(payload)
        end
      end

      private

      def process_deposit_created(payload)
        Rails.logger.info("Processing deposit create: #{payload['object']['identifier']}")
        object = payload['object']
        deposit_id = object['identifier'].split('-').last

        ActiveRecord::Base.transaction do
          deposit = CoinDeposit.find_by(id: deposit_id)
          return unless deposit && payload['isSuccess']

          coin_account = CoinAccount.find(object['accountKey'])
          create_deposit_operation(deposit, coin_account, object)
        end
      rescue ActiveRecord::RecordNotFound => e
        Rails.logger.error("Failed to find record: #{e.message}")
      rescue StandardError => e
        Rails.logger.error("Error processing deposit: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def create_deposit_operation(deposit, coin_account, object)
        CoinDepositOperation.create!(
          coin_account: coin_account,
          coin_amount: BigDecimal.safe_convert(object['amount']),
          coin_currency: object['coin'].downcase,
          coin_deposit: deposit,
          coin_fee: 0,
          platform_fee: 0,
          tx_hash: object['txHash'],
          out_index: deposit.out_index,
          status: 'completed'
        )
      end
    end
  end
end
