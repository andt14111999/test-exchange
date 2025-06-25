# frozen_string_literal: true

require_relative 'boot'

require 'rails/all'

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module Exchange
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 8.0

    # Please, add to the `ignore` list any other `lib` subdirectories that do
    # not contain `.rb` files, or that should not be reloaded or eager loaded.
    # Common ones are `templates`, `generators`, or `middleware`, for example.
    config.autoload_lib(ignore: %w[assets tasks])

    # Configuration for the application, engines, and railties goes here.
    #
    # These settings can be overridden in specific environments using the files
    # in config/environments, which are processed later.
    #
    # config.time_zone = "Central Time (US & Canada)"
    # config.eager_load_paths << Rails.root.join("extras")

    config.active_job.queue_adapter = :sidekiq
    config.active_record.encryption.primary_key = Rails.application.credentials.dig(:active_record_encryption, :primary_key)
    config.active_record.encryption.deterministic_key = Rails.application.credentials.dig(:active_record_encryption, :deterministic_key)
    config.active_record.encryption.key_derivation_salt = Rails.application.credentials.dig(:active_record_encryption, :key_derivation_salt)
    config.active_record.encryption.support_unencrypted_data = true

    # Cấu hình CORS
    config.middleware.insert_before 0, Rack::Cors do
      allow do
        origins ENV.fetch('FRONTEND_URL') { 'https://snow.exchange' }
        resource '*',
          headers: :any,
          methods: %i[get post put patch delete options],
          expose: [ 'Authorization' ]

        # Thêm cấu hình cho Active Storage
        # Proxy routes cần thiết cho cả development và production
        resource '/rails/active_storage/blobs/proxy/*',
          headers: :any,
          methods: [ :get, :options ]
        resource '/rails/active_storage/representations/proxy/*',
          headers: :any,
          methods: [ :get, :options ]

        # Redirect routes chỉ cần trong development
        if Rails.env.development?
          resource '/rails/active_storage/blobs/redirect/*',
            headers: :any,
            methods: [ :get, :options ]
          resource '/rails/active_storage/representations/redirect/*',
            headers: :any,
            methods: [ :get, :options ]
        end
      end
    end
  end
end
