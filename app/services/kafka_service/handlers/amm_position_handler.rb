# frozen_string_literal: true

module KafkaService
  module Handlers
    class AmmPositionHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        identifier = payload.dig('object', 'identifier')
        return if identifier.blank?

        Rails.logger.info("Processing amm position update: #{identifier}")
        process_amm_position_update(payload)
      end

      private

      def process_amm_position_update(payload)
        ActiveRecord::Base.transaction do
          handle_update_response(payload)
        end
      rescue ActiveRecord::RecordNotFound => e
        Rails.logger.error("Failed to find record: #{e.message}")
      rescue StandardError => e
        Rails.logger.error("Error processing position update: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def handle_update_response(payload)
        object = payload['object']
        amm_position = AmmPosition.find_by!(identifier: object['identifier'])
        return unless amm_position.persisted?

        if payload['isSuccess'].to_bool
          if object['updatedAt'].to_i < (amm_position.updated_at.to_f * 1000).to_i
            return raise 'amm position message is older than the last update'
          end

          params = extract_params_from_response(object)
          update_position_state(amm_position, params)
        else
          error_message = payload['errorMessage'] || 'Unknown error'
          amm_position.fail!("Exchange Engine: #{error_message}")
        end
      rescue StandardError => e
        Rails.logger.error("Error handling update response: #{e.message}")
      end

      def extract_params_from_response(object)
        {
          liquidity: object['liquidity'],
          amount0: object['amount0'],
          amount1: object['amount1'],
          fee_growth_inside0_last: object['feeGrowthInside0Last'],
          fee_growth_inside1_last: object['feeGrowthInside1Last'],
          tokens_owed0: object['tokensOwed0'],
          tokens_owed1: object['tokensOwed1'],
          fee_collected0: object['feeCollected0'],
          fee_collected1: object['feeCollected1'],
          error_message: object['errorMessage'],
          updated_at: Time.at(object['updatedAt'] / 1000.0),
          status: object['status'].downcase
        }.compact
      end

      def update_position_state(position, params)
        if params[:error_message].present?
          position.fail!(params[:error_message]) unless position.error?
        else
          position.update!(params)
        end
      end
    end
  end
end
