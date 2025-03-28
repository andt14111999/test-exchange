# frozen_string_literal: true

Rails.application.configure do
  # Sử dụng biến môi trường cho URL
  if Rails.env.production?
    config.action_cable.url = ENV.fetch('ACTION_CABLE_URL') { 'wss://your-domain.com/cable' }
  else
    config.action_cable.url = 'ws://localhost:3969/cable'
  end

  config.action_cable.mount_path = '/cable'

  if Rails.env.production?
    config.action_cable.allowed_request_origins = [
      ENV.fetch('FRONTEND_URL') { 'https://your-frontend-domain.com' }
    ]
  else
    config.action_cable.allowed_request_origins = [
      'http://localhost:3000',
      'http://127.0.0.1:3000',
      %r{http://localhost:*}
    ]
  end

  config.action_cable.disable_request_forgery_protection = !Rails.env.production?
  config.action_cable.redis = {
    url: ENV.fetch('REDIS_URL') { 'redis://localhost:6379/1' },
    ssl_params: { verify_mode: OpenSSL::SSL::VERIFY_NONE }
  }
end
