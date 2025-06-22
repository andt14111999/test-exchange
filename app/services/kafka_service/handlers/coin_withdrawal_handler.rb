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

        return coin_withdrawal.fail!(error_message) unless is_success

        case status.upcase
        when 'COMPLETED'
          process_completed_response(coin_withdrawal)
        when 'FAILED'
          process_failed_response(coin_withdrawal, status_explanation)
        when 'PROCESSING'
          process_processing_response(coin_withdrawal)
        when 'CANCELLED'
          process_cancelled_response(coin_withdrawal)
        end
      end

      def process_completed_response(coin_withdrawal)
        if coin_withdrawal.may_complete?
          coin_withdrawal.complete!
          Rails.logger.info("Coin withdrawal completed for withdrawal_id=#{coin_withdrawal.id} with status=#{coin_withdrawal.reload.status}")
        else
          Rails.logger.info("Coin withdrawal cannot complete for withdrawal_id=#{coin_withdrawal.id}")
        end
      end

      def process_failed_response(coin_withdrawal, status_explanation = nil)
        if coin_withdrawal.may_fail?
          coin_withdrawal.fail!(status_explanation)
          Rails.logger.info("Coin withdrawal failed for withdrawal_id=#{coin_withdrawal.id} with status=#{coin_withdrawal.reload.status}")
        else
          Rails.logger.info("Coin withdrawal cannot fail for withdrawal_id=#{coin_withdrawal.id}")
        end
      end

      def process_processing_response(coin_withdrawal)
        if coin_withdrawal.may_process?
          coin_withdrawal.process!
          Rails.logger.info("Coin withdrawal processing for withdrawal_id=#{coin_withdrawal.id} with status=#{coin_withdrawal.reload.status}")
        else
          Rails.logger.info("Coin withdrawal cannot process for withdrawal_id=#{coin_withdrawal.id}")
        end
      end

      def process_cancelled_response(coin_withdrawal)
        if coin_withdrawal.may_cancel?
          coin_withdrawal.cancel!
          Rails.logger.info("Coin withdrawal cancelled for withdrawal_id=#{coin_withdrawal.id} with status=#{coin_withdrawal.reload.status}")
        else
          Rails.logger.info("Coin withdrawal cannot cancel for withdrawal_id=#{coin_withdrawal.id}")
        end
      end
    end
  end
end
