# frozen_string_literal: true

class BalanceChannel < ApplicationCable::Channel
  def self.broadcast_to_user(user, data)
    stream_name = "balance:user_#{user.id}"
    ActionCable.server.broadcast(stream_name, data)
  end

  def subscribed
    if current_user
      stream_from "balance:user_#{current_user.id}"
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
