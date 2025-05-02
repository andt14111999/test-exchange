# frozen_string_literal: true

source 'https://rubygems.org'

################################################################################
# Core Rails & Server
################################################################################
gem 'bootsnap', require: false
gem 'pg', '~> 1.5'
gem 'puma', '>= 5.0'
gem 'rails', '~> 8.0.2'

################################################################################
# Frontend & Asset Pipeline
################################################################################
gem 'importmap-rails'
gem 'propshaft'
gem 'sassc-rails'
gem 'stimulus-rails'
gem 'turbo-rails'

################################################################################
# Authentication & Authorization
################################################################################
gem 'cancancan'
gem 'devise'
gem 'jwt'

# Social Authentication
gem 'google-id-token'
gem 'koala' # Facebook API
gem 'omniauth'
gem 'omniauth-apple'
gem 'omniauth-facebook'
gem 'omniauth-google-oauth2'
gem 'omniauth-rails_csrf_protection'

################################################################################
# API & Serialization
################################################################################
gem 'grape'
gem 'grape-active_model_serializers'
gem 'grape-entity'
gem 'grape-rails-cache'
gem 'jbuilder'
gem 'oj' # JSON parsing
gem 'rack-cors'

################################################################################
# Background Processing & Caching
################################################################################
gem 'clockwork' # Cron-like job scheduler
gem 'redis', '< 4.6'
gem 'redis-mutex', github: 'kenn/redis-mutex',
  ref: 'a460549e0c2a876fd59b9197d84b909fd72eb876'
gem 'redis-client'
gem 'nest'
gem 'sidekiq'
gem 'sidekiq-status'
gem 'solid_cable'
gem 'solid_cache'
gem 'solid_queue'

################################################################################
# Admin Interface
################################################################################
gem 'activeadmin'
gem 'activeadmin_addons', github: 'platanus/activeadmin_addons'
gem 'kaminari' # Pagination

################################################################################
# Security & Encryption
################################################################################
gem 'ed25519'
gem 'rotp', '>= 6.3.0'       # 2FA support
gem 'rqrcode', '~> 3.1'      # QR code generation

################################################################################
# Kafka & Event Processing
################################################################################
gem 'retriable'
gem 'ruby-kafka'
gem 'snappy'

################################################################################
# Monitoring & Error Tracking
################################################################################
gem 'paper_trail', '~> 16.0' # Model versioning
gem 'rollbar'

################################################################################
# Utilities
################################################################################
gem 'aasm'                    # State machines
gem 'faker'                   # Fake data generation
gem 'figaro'                  # Environment variables
gem 'httparty' # HTTP client
gem 'rails-settings-cached'   # Settings management
gem 'strong_migrations'       # Safe database migrations

# Version updates for security fixes
gem 'json', '~> 2.10.2'
gem 'net-imap', '~> 0.5.8'
gem 'nokogiri', '>= 1.18.8'
gem 'rack', '>= 3.1.12'
gem 'uri', '>= 1.0.3'

# Deployment
gem 'kamal', require: false
gem 'thruster', require: false

################################################################################
# Development & Test Environment
################################################################################
group :development, :test do
  gem 'brakeman', require: false      # Security analysis
  gem 'debug', platforms: %i[mri windows]
  gem 'pry-byebug'                    # Debugging
  gem 'rubocop-rails-omakase', require: false # Style checking
end

group :development do
  gem 'letter_opener' # Email preview
  gem 'rubocop-rake'
  gem 'rubocop-rspec'
  gem 'web-console'
end

group :test do
  # Testing Framework
  gem 'factory_bot_rails'
  gem 'rspec-json_expectations'
  gem 'rspec-rails'
  gem 'rspec-sidekiq'
  gem 'shoulda-matchers'

  # Test Coverage
  gem 'simplecov', require: false
  gem 'simplecov-json', require: false
  gem 'simplecov-lcov', require: false

  # Test Utilities
  gem 'capybara'                      # Integration testing
  gem 'database_cleaner'              # Clean test database
  gem 'pg_query', '~> 6.1.0'                      # Query analysis
  gem 'prosopite'                     # N+1 query detection
  gem 'selenium-webdriver'            # Browser automation
  gem 'vcr' # HTTP interaction recording
  gem 'webdrivers'
  gem 'webmock' # HTTP request stubbing
end

gem 'tzinfo-data', platforms: %i[windows jruby]
