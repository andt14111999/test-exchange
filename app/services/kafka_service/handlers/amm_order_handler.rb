# frozen_string_literal: true

module KafkaService
  module Handlers
    class AmmOrderHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        identifier = payload.dig('object', 'identifier')
        return if identifier.blank?

        Rails.logger.info("Processing amm order update: #{identifier}")
        process_amm_order_update(payload)
      end

      private

      def process_amm_order_update(payload)
        ActiveRecord::Base.transaction do
          handle_update_response(payload)
        end
      rescue ActiveRecord::RecordNotFound => e
        Rails.logger.error("Failed to find record: #{e.message}")
      rescue StandardError => e
        Rails.logger.error("Error processing order update: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def handle_update_response(payload)
        object = payload['object']
        amm_order = AmmOrder.find_by!(identifier: object['identifier'])
        return unless amm_order.persisted?

        if payload['isSuccess'].to_bool
          if object['updatedAt'].to_i < (amm_order.updated_at.to_f * 1000).to_i
            return raise 'amm order message is older than the last update'
          end

          params = extract_params_from_response(object)
          update_order_state(amm_order, params)
        else
          error_message = payload['errorMessage'] || 'Unknown error'
          amm_order.fail!("Exchange Engine: #{error_message}")
        end
      rescue StandardError => e
        Rails.logger.error("Error handling update response: #{e.message}")
      end

      def extract_params_from_response(object)
        {
          amount_actual: BigDecimal.safe_convert(object['amountActual']),
          amount_estimated: BigDecimal.safe_convert(object['amountEstimated']),
          amount_received: BigDecimal.safe_convert(object['amountReceived']),
          before_tick_index: object['beforeTickIndex'],
          after_tick_index: object['afterTickIndex'],
          fees: object['fees'] || {},
          error_message: object['errorMessage'],
          updated_at: Time.at(object['updatedAt'] / 1000.0),
          status: object['status']&.downcase
        }.compact
      end

      def update_order_state(order, params)
        if params[:error_message].present?
          order.fail!(params[:error_message]) unless order.error?
        elsif params[:status] == 'success'
          order.succeed! if order.processing?
          order.update!(params)
        else
          order.update!(params)
        end
      end
    end
  end
end
