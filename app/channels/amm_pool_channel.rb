# frozen_string_literal: true

class AmmPoolChannel < ApplicationCable::Channel
  def subscribed
    stream_for 'amm_pool_channel'
  end

  def unsubscribed
    stop_all_streams
  end

  def keepalive(data)
    transmit({ status: 'success', message: 'keepalive_ack', timestamp: Time.now.to_i })
  end
end
