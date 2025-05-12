# frozen_string_literal: true

module V1
  module Helpers
    module AuthHelper
      def authenticate_user!
        if api_key_auth?
          authenticate_api_key!
        else
          authenticate_jwt!
        end
      end

      def current_user
        @current_user
      end

      private

      def api_key_auth?
        headers['X-Access-Key'].present? &&
        headers['X-Signature'].present? &&
        headers['X-Timestamp'].present?
      end

      def authenticate_api_key!
        access_key = headers['X-Access-Key']
        signature = headers['X-Signature']
        timestamp = headers['X-Timestamp']

        # Ensure timestamp is recent (within 5 minutes) to prevent replay attacks
        timestamp_time = Time.at(timestamp.to_i)
        if timestamp_time < 5.minutes.ago || timestamp_time > Time.current
          error!({ status: 'error', message: 'Invalid timestamp' }, 401)
        end

        # Create the message to verify
        # For HMAC, this is typically the HTTP method, path, and timestamp
        path = env['PATH_INFO']
        method = env['REQUEST_METHOD']
        message = "#{method}#{path}#{timestamp}"

        api_key = ApiKey.find_by(access_key: access_key)
        error!({ status: 'error', message: 'Invalid API key' }, 401) unless api_key

        unless ApiKey.authenticate(access_key, signature, message)
          error!({ status: 'error', message: 'Invalid signature' }, 401)
        end

        api_key.update!(last_used_at: Time.current)
        @current_user = api_key.user
      end

      def authenticate_jwt!
        token = headers['Authorization']&.split&.last

        error!({ status: 'error', message: 'Unauthorized' }, 401) unless token

        begin
          secret = Rails.application.secret_key_base
          decoded_token = JWT.decode(
            token,
            secret,
            true,
            { algorithm: 'HS256' }
          )

          Rails.logger.info "Decoded token: #{decoded_token}"
          @current_user = ::User.find(decoded_token.first['user_id'])
        rescue JWT::DecodeError, ActiveRecord::RecordNotFound => e
          Rails.logger.error "Authentication Error: #{e.message}"
          error!({ status: 'error', message: 'Unauthorized' }, 401)
        end
      end
    end
  end
end
