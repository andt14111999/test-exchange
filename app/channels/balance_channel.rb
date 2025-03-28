# frozen_string_literal: true

class BalanceChannel < ApplicationCable::Channel
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
