# frozen_string_literal: true

class AmmOrderBroadcastService
  def self.call(user)
    new(user).call
  end

  def initialize(user)
    @user = user
  end

  def call
    broadcast_amm_orders
  end

  private

  def broadcast_amm_orders
    begin
      message = amm_order_data_with_status
      AmmOrderChannel.broadcast_to_user(@user, message)
      true
    rescue => e
      Rails.logger.error "Error broadcasting amm orders: #{e.message}"
      false
    end
  end

  def amm_order_data_with_status
    {
      status: 'success',
      data: amm_order_data
    }
  end

  def amm_order_data
    {
      amm_orders: user_amm_orders_data
    }
  end

  def user_amm_orders_data
    @user.amm_orders.includes(:amm_pool).map do |order|
      {
        id: order.id,
        identifier: order.identifier,
        status: order.status,
        amount_specified: order.amount_specified,
        amount_estimated: order.amount_estimated,
        amount_actual: order.amount_actual,
        amount_received: order.amount_received,
        zero_for_one: order.zero_for_one,
        slippage: order.slippage,
        error_message: order.error_message,
        created_at: order.created_at,
        updated_at: order.updated_at,
        amm_pool: {
          id: order.amm_pool.id,
          pair: order.amm_pool.pair,
          token0: order.amm_pool.token0,
          token1: order.amm_pool.token1
        }
      }
    end
  end
end
