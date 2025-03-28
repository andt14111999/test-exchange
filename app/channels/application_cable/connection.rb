module ApplicationCable
  class Connection < ActionCable::Connection::Base
    identified_by :current_user

    def connect
      self.current_user = find_verified_user
    end

    def disconnect
    end

    private

    def find_verified_user
      if verified_user = env['warden']&.user
        return verified_user
      end

      token = request.params['token']
      if token.present?
        begin
          payload = JWT.decode(token, Rails.application.secrets.secret_key_base, true, algorithm: 'HS256').first
          user_id = payload['user_id']
          if user_id
            user = User.find_by(id: user_id)
            return user if user
          end
        rescue JWT::DecodeError, StandardError
          # Token invalid
        end
      end

      reject_unauthorized_connection
    end
  end
end
