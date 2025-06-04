# frozen_string_literal: true

class TradeChannel < ApplicationCable::Channel
  def self.broadcast_to_trade(trade, data)
    broadcast_to(trade, data)
  end

  def subscribed
    trade = Trade.where(id: params[:trade_id])
               .where('buyer_id = :user_id OR seller_id = :user_id', user_id: current_user.id)
               .first

    if trade
      stream_for trade
    else
      reject
    end
  end

  def unsubscribed
    stop_all_streams
  end

  def keepalive(_data)
    transmit({ status: 'success', message: 'keepalive_ack', timestamp: Time.current.to_i })
  end
end
