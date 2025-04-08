# frozen_string_literal: true

class AmmPoolChannel < ApplicationCable::Channel
  def self.channel_name
    self.name.underscore
  end

  def subscribed
    stream_for self.class.channel_name
  end

  def unsubscribed
    stop_all_streams
  end

  def keepalive(data)
    transmit({ status: 'success', message: 'keepalive_ack', timestamp: Time.now.to_i })
  end
end
