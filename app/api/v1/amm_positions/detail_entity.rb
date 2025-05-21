# frozen_string_literal: true

module V1
  module AmmPositions
    class DetailEntity < Entity
      format_with(:decimal) { |number| number.to_s }

      # Include all fields from the base Entity
      expose :id
      expose :identifier
      expose :pool_pair
      expose :status
      expose :error_message
      expose :tick_lower_index
      expose :tick_upper_index

      expose :liquidity, format_with: :decimal
      expose :slippage, format_with: :decimal
      expose :amount0, format_with: :decimal
      expose :amount1, format_with: :decimal
      expose :amount0_initial, format_with: :decimal
      expose :amount1_initial, format_with: :decimal

      expose :fee_growth_inside0_last, format_with: :decimal
      expose :fee_growth_inside1_last, format_with: :decimal
      expose :tokens_owed0, format_with: :decimal
      expose :tokens_owed1, format_with: :decimal
      expose :fee_collected0, format_with: :decimal
      expose :fee_collected1, format_with: :decimal

      # Additional fields for detail view
      expose :amount0_withdrawal, format_with: :decimal
      expose :amount1_withdrawal, format_with: :decimal
      expose :estimate_fee_token0, format_with: :decimal
      expose :estimate_fee_token1, format_with: :decimal
      expose :apr, format_with: :decimal

      # Total estimated fee converted to token0 for easier frontend display
      expose :total_estimate_fee_in_token0, format_with: :decimal

      expose :created_at do |position|
        position.created_at.to_i
      end

      expose :updated_at do |position|
        position.updated_at.to_i
      end
    end
  end
end
