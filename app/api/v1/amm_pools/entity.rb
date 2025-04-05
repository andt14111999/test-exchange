# frozen_string_literal: true

module V1
  module AmmPools
    class Entity < Grape::Entity
      expose :id
      expose :pair
      expose :token0
      expose :token1
      expose :tick_spacing
      expose :fee_percentage
      expose :fee_protocol_percentage
      expose :current_tick
      expose :sqrt_price
      expose :price
      expose :liquidity
      expose :status
      expose :status_explanation

      expose :volume_token0
      expose :volume_token1
      expose :volume_usd
      expose :tx_count

      expose :total_value_locked_token0
      expose :total_value_locked_token1

      expose :created_at do |pool|
        pool.created_at.to_i
      end
      expose :updated_at do |pool|
        pool.updated_at.to_i
      end
    end
  end
end
