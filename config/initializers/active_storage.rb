# frozen_string_literal: true

Rails.application.configure do
  if Rails.env.development?
    Rails.application.routes.default_url_options[:host] = 'localhost'
    Rails.application.routes.default_url_options[:port] = 3969
    Rails.application.routes.default_url_options[:protocol] = 'http'
  end
end
