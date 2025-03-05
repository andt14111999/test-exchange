# frozen_string_literal: true

class AuthHash
  Info = Struct.new(:email, :name, :image, keyword_init: true)
  Credentials = Struct.new(:token, :refresh_token, :expires_at, keyword_init: true)
  Extra = Struct.new(:raw_info, keyword_init: true)

  attr_reader :provider, :uid, :info, :credentials, :extra

  def initialize(provider:, uid:, info:, credentials:, extra:)
    @provider = provider
    @uid = uid
    @info = Info.new(info)
    @credentials = Credentials.new(credentials)
    @extra = Extra.new(raw_info: extra)
  end
end
