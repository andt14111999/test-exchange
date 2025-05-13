# frozen_string_literal: true

module ApiKeyHelpers
  # Provide a test encryption key for attr_encrypted
  def with_seed_encryption_key
    original_encryption_key = ENV['SEED_ENCRYPTION_KEY']
    # Must be exactly 32 bytes for attr_encrypted
    ENV['SEED_ENCRYPTION_KEY'] = '12345678901234567890123456789012'
    yield
  ensure
    ENV['SEED_ENCRYPTION_KEY'] = original_encryption_key
  end
end

RSpec.configure do |config|
  config.include ApiKeyHelpers

  # Set up encryption key for API key related tests
  config.around do |example|
    with_seed_encryption_key { example.run }
  end
end
