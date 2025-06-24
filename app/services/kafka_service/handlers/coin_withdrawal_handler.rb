# frozen_string_literal: true

module KafkaService
  module Handlers
    class CoinWithdrawalHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        process_transaction_response(payload)
      end

      private

      def process_transaction_response(payload)
        object = payload['object']
        return unless object

        identifier = object['identifier']
        return unless identifier

        coin_withdrawal = CoinWithdrawal.find_by(id: identifier)
        return unless coin_withdrawal

        status = object['status']
        status_explanation = object['statusExplanation']
        is_success = payload['isSuccess']
        error_message = payload['errorMessage']
        current_status = coin_withdrawal.status

        Rails.logger.info("Processing Kafka event for withdrawal_id=#{coin_withdrawal.id}, current status: #{current_status}, kafka status: #{status}, isSuccess: #{is_success}")

        unless is_success
          coin_withdrawal.update(status: 'failed', explanation: error_message)
          Rails.logger.info("Coin withdrawal failed for withdrawal_id=#{coin_withdrawal.id}, error: #{error_message}")
          return
        end

        # Update status directly from Kafka
        update_params = { status: status.downcase }
        update_params[:explanation] = status_explanation if status_explanation.present? && status.upcase == 'FAILED'

        coin_withdrawal.update(update_params)
        Rails.logger.info("Coin withdrawal status updated for withdrawal_id=#{coin_withdrawal.id} from #{current_status} to #{status.downcase}")
      end
    end
  end
end
