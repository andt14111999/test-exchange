# frozen_string_literal: true

class AmmPositionBroadcastService
  def self.call(user)
    new(user).call
  end

  def initialize(user)
    @user = user
  end

  def call
    broadcast_amm_positions
  end

  private

  def broadcast_amm_positions
    begin
      message = amm_position_data_with_status
      AmmPositionChannel.broadcast_to_user(@user, message)
      true
    rescue => e
      Rails.logger.error "Error broadcasting amm positions: #{e.message}"
      false
    end
  end

  def amm_position_data_with_status
    {
      status: 'success',
      data: amm_position_data
    }
  end

  def amm_position_data
    {
      amm_positions: user_amm_positions_data
    }
  end

  def user_amm_positions_data
    @user.amm_positions.includes(:amm_pool).map do |position|
      {
        id: position.id,
        identifier: position.identifier,
        status: position.status,
        liquidity: position.liquidity,
        amount0: position.amount0,
        amount1: position.amount1,
        amount0_initial: position.amount0_initial,
        amount1_initial: position.amount1_initial,
        tick_lower_index: position.tick_lower_index,
        tick_upper_index: position.tick_upper_index,
        fee_collected0: position.fee_collected0,
        fee_collected1: position.fee_collected1,
        tokens_owed0: position.tokens_owed0,
        tokens_owed1: position.tokens_owed1,
        estimate_fee_token0: position.estimate_fee_token0,
        estimate_fee_token1: position.estimate_fee_token1,
        apr: position.apr,
        created_at: position.created_at,
        updated_at: position.updated_at,
        amm_pool: {
          id: position.amm_pool.id,
          pair: position.amm_pool.pair,
          token0: position.amm_pool.token0,
          token1: position.amm_pool.token1,
          price: position.amm_pool.price
        }
      }
    end
  end
end
