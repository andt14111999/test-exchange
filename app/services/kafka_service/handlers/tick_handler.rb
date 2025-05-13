# frozen_string_literal: true

module KafkaService
  module Handlers
    class TickHandler < BaseHandler
      def handle(payload)
        return if payload.nil?
        Rails.logger.info("Processing tick update: #{payload}")

        pool_pair = payload['poolPair']
        return if pool_pair.blank?

        process_tick_update(payload)
      end

      private

      def process_tick_update(payload)
        ActiveRecord::Base.transaction do
          handle_update_response(payload)
        end
      rescue ActiveRecord::RecordNotFound => e
        Rails.logger.error("Failed to find record: #{e.message}")
      rescue StandardError => e
        Rails.logger.error("Error processing tick update: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def handle_update_response(payload)
        # Find or create the tick
        tick = find_or_create_tick(payload)
        return unless tick.persisted?

        if payload['updatedAt'].to_i < (tick.updated_at.to_f * 1000).to_i
          return raise 'tick message is older than the last update'
        end

        params = extract_params_from_response(payload)
        tick.update!(params)
      rescue StandardError => e
        Rails.logger.error("Error handling tick update response: #{e.message}")
      end

      def find_or_create_tick(payload)
        pool_pair = payload['poolPair']
        tick_index = payload['tickIndex']
        tick_key = "#{pool_pair}-#{tick_index}"

        amm_pool = ::AmmPool.find_by(pair: pool_pair)
        return nil unless amm_pool.present?

        tick = ::Tick.find_by(tick_key: tick_key)
        return tick if tick.present?

        # If tick doesn't exist, create it with all attributes
        ::Tick.create!(
          pool_pair: pool_pair,
          tick_index: tick_index,
          tick_key: tick_key,
          amm_pool: amm_pool,
          liquidity_gross: payload['liquidityGross'],
          liquidity_net: payload['liquidityNet'],
          fee_growth_outside0: payload['feeGrowthOutside0'],
          fee_growth_outside1: payload['feeGrowthOutside1'],
          initialized: payload['initialized'] || false,
          tick_initialized_timestamp: payload['tickInitializedTimestamp'],
          created_at_timestamp: payload['createdAt'],
          updated_at_timestamp: payload['updatedAt']
        )
      end

      def extract_params_from_response(payload)
        {
          liquidity_gross: payload['liquidityGross'],
          liquidity_net: payload['liquidityNet'],
          fee_growth_outside0: payload['feeGrowthOutside0'],
          fee_growth_outside1: payload['feeGrowthOutside1'],
          initialized: payload['initialized'],
          tick_initialized_timestamp: payload['tickInitializedTimestamp'],
          created_at_timestamp: payload['createdAt'],
          updated_at_timestamp: payload['updatedAt'],
          updated_at: payload['updatedAt'] ? Time.at(payload['updatedAt'] / 1000.0) : nil
        }.compact
      end
    end
  end
end
