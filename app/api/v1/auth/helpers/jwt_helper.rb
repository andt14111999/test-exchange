# frozen_string_literal: true

module V1
  module Auth
    module Helpers
      module JwtHelper
        def generate_jwt_token(user)
          secret = Rails.application.secret_key_base
          payload = {
            user_id: user.id,
            email: user.email,
            exp: 24.hours.from_now.to_i
          }
          JWT.encode(payload, secret, 'HS256')
        end
      end
    end
  end
end
