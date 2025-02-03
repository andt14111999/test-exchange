# frozen_string_literal: true

VCR.configure do |config|
  config.allow_http_connections_when_no_cassette = true
  config.cassette_library_dir = 'spec/fixtures/vcr_cassettes'
  config.hook_into :webmock
  config.configure_rspec_metadata!

  config.before_record do |i|
    i.response.headers.delete("Set-Cookie")
    i.request.headers.delete("Authorization")
    u = URI.parse(i.request.uri)
    i.request.uri.sub!(%r{://.*#{Regexp.escape(u.host)}}, "://#{u.host}")
  end

  %w[
    REMITANO_ACCESS_KEY REMITANO_SECRET_KEY REMITANO_TWO_FA_SECRET
    TOGGL_WORKSPACE_ID TOGGL_TOKEN TOGGL_USER_AGENT TOGGL_ORGANIZATION_ID
  ].each do |key|
    config.filter_sensitive_data("<#{key}>") { ENV.fetch(key, nil) }
  end
end
