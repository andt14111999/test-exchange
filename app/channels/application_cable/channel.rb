# frozen_string_literal: true

module ApplicationCable
  class Channel < ActionCable::Channel::Base
    def disconnect
      stop_all_streams
    end

    private

    def current_user
      connection.current_user
    end

    def env
      connection.env
    end

    def reject_unauthorized_connection
      raise ActionCable::Connection::Authorization::UnauthorizedError
    end
  end
end
