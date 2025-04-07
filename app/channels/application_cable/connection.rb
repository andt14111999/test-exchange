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
      # Try to find user from Devise session
      if verified_user = env['warden']&.user
        return verified_user
      end

      # Try to find user from JWT token in params or headers
      token = find_token
      return reject_unauthorized_connection unless token.present?

      begin
        payload = JWT.decode(
          token,
          Rails.application.credentials.secret_key_base,
          true,
          { algorithm: 'HS256' }
        ).first

        user_id = payload['user_id']
        user = user_id && User.find_by(id: user_id)
        return user if user
      rescue JWT::DecodeError => e
        Rails.logger.error("JWT decode error: #{e.message}")
      rescue StandardError => e
        Rails.logger.error("Connection error: #{e.message}")
      end

      reject_unauthorized_connection
    end

    def find_token
      # Try token from params
      token = request.params['token']
      return token if token.present?

      # Try token from Authorization header
      auth_header = request.env['HTTP_AUTHORIZATION']
      return nil unless auth_header.present?

      # Extract token from "Bearer <token>" format
      auth_header.split(' ').last
    end
  end
end
