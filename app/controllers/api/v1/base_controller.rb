# frozen_string_literal: true

module Api
  module V1
    class BaseController < ActionController::API
      include ActionController::MimeResponds
      include ActionController::Cookies
      include ActionController::RequestForgeryProtection

      protect_from_forgery with: :null_session

      private

      def authenticate_user!
        token = request.headers['HTTP_AUTHORIZATION']&.split&.last ||
                request.headers['Authorization']&.split&.last

        return render_unauthorized unless token

        begin
          secret = Rails.application.secret_key_base
          decoded_token = JWT.decode(
            token,
            secret,
            true,
            { algorithm: 'HS256' }
          )

          Rails.logger.info "Decoded token: #{decoded_token}"
          @current_user = User.find(decoded_token.first['user_id'])
        rescue JWT::DecodeError, ActiveRecord::RecordNotFound => e
          Rails.logger.error "Authentication Error: #{e.message}"
          render_unauthorized
        end
      end

      attr_reader :current_user

      def render_unauthorized
        render json: { status: 'error', message: 'Unauthorized' }, status: :unauthorized
      end
    end
  end
end
