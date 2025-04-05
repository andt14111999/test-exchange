# frozen_string_literal: true

RSpec.configure do |config|
  config.before do
    allow_any_instance_of(User).to receive(:create_default_accounts)
  end

  config.after do
    User.set_callback(:create, :after, :create_default_accounts)
  end
end
