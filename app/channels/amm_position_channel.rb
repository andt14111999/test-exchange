# frozen_string_literal: true

class AmmPositionChannel < ApplicationCable::Channel
  def self.broadcast_to_user(user, data)
    broadcast_to(user, data)
  end

  def subscribed
    if current_user
      stream_for current_user
    else
      reject
    end
  end

  def unsubscribed
    stop_all_streams
  end

  def keepalive(data)
    transmit({ status: 'success', message: 'keepalive_ack', timestamp: Time.now.to_i })
  end
end
