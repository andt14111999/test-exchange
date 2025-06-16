# frozen_string_literal: true

module KafkaService
  module Handlers
    class TransactionResponseHandler
      def handle(payload)
        return if payload['isSuccess']

        error_message = payload['errorMessage']
        record = find_record(payload['actionType'], payload['recordId'])

        log_error_to_rollbar(payload['actionType'], payload['recordId'], error_message)

        return unless record

        handle_failure_with_status_change(record, error_message)
      end

      private

      def handle_failure_with_status_change(record, error_message)
        case record.class.name
        when 'CoinTransaction', 'AmmPool', 'AmmPosition', 'MerchantEscrow', 'AmmOrder', 'Trade', 'BalanceLock'
          # Models with AASM - use transaction_fail! event
          record.transaction_fail!(error_message)
        when 'Offer'
          # Models without AASM - just update error_message
          record.update(error_message: error_message)
        end
      end

      def find_record(action_type, record_id)
        case action_type
        when 'CoinTransaction'
          CoinTransaction.find_by(id: record_id)
        when 'AmmPool'
          AmmPool.find_by(id: record_id)
        when 'AmmPosition'
          AmmPosition.find_by(id: record_id)
        when 'MerchantEscrow'
          MerchantEscrow.find_by(id: record_id)
        when 'AmmOrder'
          AmmOrder.find_by(id: record_id)
        when 'Trade'
          Trade.find_by(id: record_id)
        when 'Offer'
          Offer.find_by(id: record_id)
        when 'BalancesLock'
          BalanceLock.find_by(id: record_id)
        end
      end

      def log_error_to_rollbar(action_type, record_id, error_message)
        Rollbar.error(
          'Transaction failed',
          action_type: action_type,
          record_id: record_id,
          error_message: error_message
        )
      end
    end
  end
end
