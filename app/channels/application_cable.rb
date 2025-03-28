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
  end

  class Connection < ActionCable::Connection::Base
    identified_by :current_user

    def connect
      token = request.params[:token] || request.headers['Authorization']&.split(' ')&.last

      if token.present?
        begin
          decoded_token = JWT.decode(token, Rails.application.secret_key_base, true, { algorithm: 'HS256' })[0]
          self.current_user = User.find(decoded_token['user_id'])
        rescue JWT::DecodeError, ActiveRecord::RecordNotFound
          reject_unauthorized_connection
        end
      else
        reject_unauthorized_connection
      end
    end
  end
end
