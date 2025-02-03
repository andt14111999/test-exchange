# frozen_string_literal: true

require 'sidekiq'
require 'sidekiq-status'

if ENV['REDIS_URL']
  Sidekiq.configure_server do |config|
    config.redis = { url: ENV['REDIS_URL'], network_timeout: 5 }

    Sidekiq::Status.configure_server_middleware config
    Sidekiq::Status.configure_client_middleware config
  end

  Sidekiq.configure_client do |config|
    config.redis = { url: ENV['REDIS_URL'], network_timeout: 5 }

    Sidekiq::Status.configure_client_middleware config
  end
end
