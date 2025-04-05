# frozen_string_literal: true

RSpec.configure do |config|
  config.before do |example|
    # Only disable the callback if we're not in a callback test
    unless example.metadata[:test_callbacks]
      allow_any_instance_of(User).to receive(:create_default_accounts)
    end
  end

  config.after do
    User.set_callback(:create, :after, :create_default_accounts)
  end
end
