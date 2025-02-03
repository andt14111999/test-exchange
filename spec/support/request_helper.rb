# frozen_string_literal: true

RSpec.configure do |config|
  config.include Warden::Test::Helpers,             type: :request
  config.include Warden::Test::Helpers,             type: :feature
  config.include Devise::Test::IntegrationHelpers
  # Warden.test_mode!

  config.after(:each, type: :request) do
    Warden.test_reset!
  end
  config.after(:each, type: :feature) do
    Warden.test_reset!
  end
end
