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
          update_kafka_event_message(payload, "Coin withdrawal failed: #{error_message}")
          Rails.logger.info("Coin withdrawal failed for withdrawal_id=#{coin_withdrawal.id}, error: #{error_message}")
          return
        end

        # Update status directly from Kafka
        update_params = { status: status.downcase }
        update_params[:explanation] = status_explanation if status_explanation.present? && status.upcase == 'FAILED'

        success = coin_withdrawal.update(update_params)
        if success
          update_kafka_event_message(payload, "Coin withdrawal status updated from #{current_status} to #{status.downcase}")
          Rails.logger.info("Coin withdrawal status updated for withdrawal_id=#{coin_withdrawal.id} from #{current_status} to #{status.downcase}")
        else
          update_kafka_event_message(payload, "Coin withdrawal status update failed: #{coin_withdrawal.errors.full_messages.join(', ')}")
          Rails.logger.error("Coin withdrawal status update failed for withdrawal_id=#{coin_withdrawal.id}, error: #{coin_withdrawal.errors.full_messages}")
        end
      end

      def update_kafka_event_message(payload, message)
        event_id = payload['inputEventId'] || payload['eventId'] || payload['messageId']
        return unless event_id

        kafka_event = KafkaEvent.find_by(
          event_id: event_id,
          topic_name: KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE
        )
        return unless kafka_event

        timestamp = Time.current.strftime('%Y-%m-%d %H:%M:%S')
        new_message = "[#{timestamp}] #{message}"

        if kafka_event.process_message.present?
          kafka_event.update(process_message: "#{kafka_event.process_message}\n#{new_message}")
        else
          kafka_event.update(process_message: new_message)
        end
      rescue StandardError => e
        Rails.logger.error("Failed to update KafkaEvent process_message: #{e.message}")
      end
    end
  end
end
