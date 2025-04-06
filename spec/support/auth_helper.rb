# frozen_string_literal: true

module AuthHelper
  def auth_headers(user)
    token = JWT.encode(
      {
        user_id: user.id,
        email: user.email,
        exp: 24.hours.from_now.to_i
      },
      Rails.application.secret_key_base,
      'HS256'
    )

    { 'Authorization' => "Bearer #{token}" }
  end
end

RSpec.configure do |config|
  config.include AuthHelper, type: :request
end
