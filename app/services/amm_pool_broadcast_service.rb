# frozen_string_literal: true

class AmmPoolBroadcastService
  def self.call(amm_pool)
    new(amm_pool).call
  end

  def initialize(amm_pool)
    @amm_pool = amm_pool
  end

  def call
    broadcast_amm_pool
  end

  private

  def broadcast_amm_pool
    begin
      AmmPoolChannel.broadcast_to('amm_pool_channel', amm_pool_data)
      true
    rescue => e
      Rails.logger.error("Failed to broadcast amm pool: #{e.message}")
      false
    end
  end

  def amm_pool_data
    {
      status: 'success',
      data: {
        id: @amm_pool.id,
        pair: @amm_pool.pair,
        token0: @amm_pool.token0,
        token1: @amm_pool.token1,
        tick_spacing: @amm_pool.tick_spacing,
        fee_percentage: @amm_pool.fee_percentage,
        current_tick: @amm_pool.current_tick,
        sqrt_price: @amm_pool.sqrt_price,
        price: @amm_pool.price,
        apr: @amm_pool.apr,
        tvl_in_token0: @amm_pool.tvl_in_token0,
        tvl_in_token1: @amm_pool.tvl_in_token1,
        created_at: @amm_pool.created_at.to_i,
        updated_at: @amm_pool.updated_at.to_i
      }
    }
  end
end
