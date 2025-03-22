# frozen_string_literal: true

class PostbackService
  class ConfigurationError < StandardError; end
  class RequestError < StandardError; end

  include HTTParty
  default_timeout 30

  attr_reader :target_url, :payload, :options

  def initialize(target_url:, payload:, options: {})
    @target_url = target_url
    @payload = payload
    @options = options
  end

  def post
    sign_payload
    response = HTTParty.post(
      target_url,
      body: json_payload,
      headers: headers,
      timeout: options.fetch(:timeout, 30)
    )

    validate_response(response)
    response
  end

  private

  def sign_payload
    timestamp = Time.current.to_i.to_s
    @json_payload = payload.to_json
    message = JSON.generate(payload.merge(timestamp: timestamp))

    @signature = signing_key.sign(message).unpack1('H*')
    @timestamp = timestamp
  end

  attr_reader :json_payload

  def headers
    {
      'Content-Type': 'application/json',
      'X-Signature': @signature,
      'X-Timestamp': @timestamp,
      'X-App-Name': 'coin-portal'
    }
  end

  def signing_key
    @signing_key ||= begin
      private_key = ENV.fetch('EXCHANGE_SIGNING_KEY') do
        raise ConfigurationError, 'EXCHANGE_SIGNING_KEY environment variable is not set'
      end

      begin
        Ed25519::SigningKey.new([ private_key ].pack('H*'))
      rescue StandardError => e
        raise ConfigurationError, "Invalid ED25519 private key: #{e.message}"
      end
    end
  end

  def public_key_hex
    @public_key_hex ||= signing_key.verify_key.to_bytes.unpack1('H*')
  end

  def validate_response(response)
    return if response.success?

    message = "Request failed with status #{response.code}: #{response.body}"
    Rails.logger.error(message)
    raise RequestError, message
  end
end
