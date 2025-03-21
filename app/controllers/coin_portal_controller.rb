# frozen_string_literal: true

class CoinPortalController < ApplicationController
  skip_before_action :verify_authenticity_token, raise: false
  before_action :authenticate_request
  HASH = 'fc55e3a2b6ddf73572563e7344c9bdf8'

  wrap_parameters format: []

  def index
    case params[:type]
    when 'deposit'
      handle_deposit
    else
      render plain: 'unsupported type', status: :bad_request
    end
  end

  protected

  def handle_deposit
    coin_currency = CoinAccount::PORTAL_COIN_TO_COIN_CURRENCY[params[:coin]]
    coin_account = CoinAccount.find(address: params[:address], coin_currency: coin_currency)

    if coin_account
      dep, ok = coin_account.handle_deposit(params)
      if ok
        render json: { created_at: dep.created_at.to_i }
      else
        render plain: dep, status: :bad_request
      end
    else
      render plain: 'Account not found', status: :bad_request
    end
  end

  def authenticate_request
    signature = request.headers['X-Signature']
    return render plain: 'Missing signature', status: :unauthorized unless signature

    timestamp = request.headers['X-Timestamp']
    return render plain: 'Missing timestamp', status: :unauthorized unless timestamp

    binary_signature = [ signature ].pack('H*')
    public_key_hex = ENV.fetch('COIN_PORTAL_VERIFYING_KEY', nil)
    return render plain: 'Server configuration error', status: :internal_server_error unless public_key_hex

    begin
      verify_key = Ed25519::VerifyKey.new([ public_key_hex ].pack('H*'))

      parsed_params = request.request_parameters
      message_data = parsed_params.merge(timestamp: timestamp)
      message = message_data.to_json

      verify_key.verify(binary_signature, message)
    rescue Ed25519::VerifyError => e
      Rails.logger.error("Ed25519 authentication error: #{e.message}")
      render plain: 'Invalid signature', status: :unauthorized
    rescue StandardError => e
      Rails.logger.error("Standard authentication error: #{e.message}")
      render plain: 'Authentication error', status: :unauthorized
    end
  end
end
