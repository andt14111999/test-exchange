# frozen_string_literal: true

module KafkaService
  module Handlers
    class AmmPoolHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        Rails.logger.info("Processing amm pool update: #{payload['pair']}")

        case payload['operationType']
        when Config::OperationTypes::AMM_POOL_UPDATE, Config::OperationTypes::AMM_POOL_CREATE
          process_amm_pool_update(payload)
        end
      end

      private

      def process_amm_pool_update(payload)
        Rails.logger.info("Processing amm pool update: #{payload['pair']}")

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
        amm_pool = AmmPool.find(payload['actionId'])
        return unless amm_pool.persisted?

        if payload['isSuccess'].to_bool
          object = payload['object']
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
          fee_percentage: object['feePercentage'],
          fee_protocol_percentage: object['feeProtocolPercentage'],
          current_tick: object['currentTick'],
          sqrt_price: object['currentSqrtPrice'],
          price: object['currentPrice'],
          liquidity: object['currentLiquidity'],
          fee_growth_global0: object['feeGrowthGlobal0'],
          fee_growth_global1: object['feeGrowthGlobal1'],
          protocol_fees0: object['protocolFees0'],
          protocol_fees1: object['protocolFees1'],
          volume_token0: object['volumeToken0'],
          volume_token1: object['volumeToken1'],
          volume_usd: object['volumeUsd'],
          total_value_locked_token0: object['totalValueLockedToken0'],
          total_value_locked_token1: object['totalValueLockedToken1'],
          status_explanation: object['statusExplanation'],
          updated_at: Time.at(object['updatedAt'] / 1000.0),
          status: object['isActive'] ? 'active' : 'inactive'
        }.compact
      end
    end
  end
end
