# frozen_string_literal: true

module KafkaService
  module Handlers
    class AmmPoolHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        pair = payload.dig('object', 'pair')
        return if pair.blank?

        Rails.logger.info("Processing amm pool update: #{pair}")
        process_amm_pool_update(payload)
      end

      private

      def process_amm_pool_update(payload)
        ActiveRecord::Base.transaction do
          handle_update_response(payload)
        end
      rescue ActiveRecord::RecordNotFound => e
        Rails.logger.error("Failed to find record: #{e.message}")
      rescue StandardError => e
        Rails.logger.error("Error processing deposit: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      private

      def handle_update_response(payload)
        object = payload['object']
        amm_pool = AmmPool.find_by!(pair: object['pair'])
        return unless amm_pool.persisted?

        if payload['isSuccess'].to_bool
          if object['updatedAt'].to_i < (amm_pool.updated_at.to_f * 1000).to_i
            return raise 'amm pool message is older than the last update'
          end

          params = extract_params_from_response(object)
          amm_pool.update!(params)
        else
          error_message = payload['errorMessage'] || 'Unknown error'
          amm_pool.update!(status_explanation: "Exchange Engine: #{error_message}")
        end
      rescue StandardError => e
        Rails.logger.error("Error handling update response: #{e.message}")
      end

      def extract_params_from_response(object)
        {
          fee_percentage: BigDecimal.safe_convert(object['feePercentage']),
          fee_protocol_percentage: BigDecimal.safe_convert(object['feeProtocolPercentage']),
          current_tick: object['currentTick'],
          sqrt_price: BigDecimal.safe_convert(object['sqrtPrice']),
          init_price: BigDecimal.safe_convert(object['initPrice']),
          price: BigDecimal.safe_convert(object['price']),
          liquidity: BigDecimal.safe_convert(object['liquidity']),
          fee_growth_global0: BigDecimal.safe_convert(object['feeGrowthGlobal0']),
          fee_growth_global1: BigDecimal.safe_convert(object['feeGrowthGlobal1']),
          protocol_fees0: BigDecimal.safe_convert(object['protocolFees0']),
          protocol_fees1: BigDecimal.safe_convert(object['protocolFees1']),
          volume_token0: BigDecimal.safe_convert(object['volumeToken0']),
          volume_token1: BigDecimal.safe_convert(object['volumeToken1']),
          volume_usd: BigDecimal.safe_convert(object['volumeUsd']),
          tx_count: object['txCount'],
          total_value_locked_token0: BigDecimal.safe_convert(object['totalValueLockedToken0']),
          total_value_locked_token1: BigDecimal.safe_convert(object['totalValueLockedToken1']),
          status_explanation: object['statusExplanation'],
          updated_at: Time.at(object['updatedAt'] / 1000.0),
          status: object['isActive'] ? 'active' : 'inactive'
        }.compact
      end
    end
  end
end
