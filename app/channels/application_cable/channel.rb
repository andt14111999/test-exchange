# frozen_string_literal: true

module ApplicationCable
  class Channel < ActionCable::Channel::Base
    def connect
      self.current_user = find_verified_user
    end

    def disconnect
      stop_all_streams
    end

    private

    def find_verified_user
      if verified_user = env['warden'].user
        verified_user
      else
        reject_unauthorized_connection
      end
    end

    def current_user
      connection.current_user
    end
  end
end
