# frozen_string_literal: true

RSpec.configure do |config|
  DatabaseCleaner.strategy = :deletion

  config.before do
    DatabaseCleaner.start
  end

  config.after do
    DatabaseCleaner.clean
  end
end
