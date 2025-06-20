# frozen_string_literal: true

RSpec.configure do |config|
  config.around(:each, sidekiq: :inline) do |example|
    Sidekiq::Testing.inline! do
      example.run
    end
  end
end
