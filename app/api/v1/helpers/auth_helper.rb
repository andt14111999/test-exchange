# frozen_string_literal: true

module V1
  module Helpers
    module AuthHelper
      def authenticate_user!
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

      def current_user
        @current_user
      end
    end
  end
end
