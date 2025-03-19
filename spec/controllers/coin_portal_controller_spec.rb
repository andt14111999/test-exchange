# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinPortalController, type: :request do
  describe '#index' do
    it 'returns bad request for unsupported type' do
      params = { amount: 100 }
      headers = setup_valid_authentication(request_params: params)
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/withdraw', headers: headers, params: params, as: :json
      expect(response).to have_http_status(:bad_request)
      expect(response.body).to eq('unsupported type')
    end
  end

  describe '#authenticate_request' do
    it 'returns unauthorized when signature header is missing' do
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit'
      expect(response).to have_http_status(:unauthorized)
      expect(response.body).to eq('Missing signature')
    end

    it 'returns unauthorized when timestamp header is missing' do
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit',
        headers: { 'X-Signature' => 'some_signature' }
      expect(response).to have_http_status(:unauthorized)
      expect(response.body).to eq('Missing timestamp')
    end

    it 'returns unauthorized for invalid signature format' do
      headers = {
        'X-Signature' => 'invalid_hex',
        'X-Timestamp' => Time.zone.now.to_i.to_s
      }
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit', headers: headers
      expect(response).to have_http_status(:unauthorized)
      expect(response.body).to eq('Authentication error')
    end

    it 'returns server error when verifying key is not configured' do
      allow(ENV).to receive(:fetch).with('COIN_PORTAL_VERIFYING_KEY', nil).and_return(nil)

      headers = {
        'X-Signature' => '68656c6c6f', # "hello" in hex
        'X-Timestamp' => Time.zone.now.to_i.to_s
      }

      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit', headers: headers
      expect(response).to have_http_status(:internal_server_error)
      expect(response.body).to eq('Server configuration error')
    end

    it 'returns ok when signature is valid' do
      params = { amount: 100 }
      headers = setup_valid_authentication(request_params: params)
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit', headers: headers, params: params, as: :json
      expect(response).to have_http_status(:bad_request)
      expect(response.body).to eq('Account not found')
    end
  end

  describe '#handle_deposit' do
    it 'returns bad request when account is not found' do
      headers = setup_valid_authentication(request_params: { address: 'non_existent_address' })

      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit',
        headers: headers,
        params: { address: 'non_existent_address' },
        as: :json

      expect(response).to have_http_status(:bad_request)
      expect(response.body).to eq('Account not found')
    end

    it 'handles successful deposit' do
      user = create(:user)
      coin_account = user.coin_accounts.first

      params = {
        address: coin_account.address,
        amount: '100',
        coin: coin_account.coin_currency,
        tx_hash: '0x1234567890abcdef',
        out_index: 0
      }
      headers = setup_valid_authentication(request_params: params)
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit',
        params: params,
        headers: headers,
        as: :json

      expect(response).to have_http_status(:success)
      created_deposit = CoinDeposit.last
      expect(created_deposit.coin_account).to eq(coin_account)
      expect(created_deposit.coin_currency).to eq(coin_account.coin_currency)
      expect(created_deposit.coin_amount).to eq(100)
      expect(created_deposit.tx_hash).to eq('0x1234567890abcdef')
      expect(created_deposit.out_index).to eq(0)
      expect(created_deposit.confirmations_count).to eq(0)
      expect(created_deposit.required_confirmations_count).to eq(0)
      expect(response.parsed_body).to eq('created_at' => created_deposit.created_at.to_i)
    end

    it 'returns bad request for failed deposit' do
      user = create(:user)
      coin_account = user.coin_accounts.first

      params = {
        address: coin_account.address,
        amount: '100',
        coin: coin_account.coin_currency,
        out_index: 0
      }
      headers = setup_valid_authentication(request_params: params)
      post '/coin_portal/fc55e3a2b6ddf73572563e7344c9bdf8/deposit',
        params: params,
        headers: headers,
        as: :json

      expect(response).to have_http_status(:bad_request)
      expect(response.parsed_body).to eq('Tx hash can\'t be blank')
    end
  end

  private

  def setup_valid_authentication(request_params: {})
    signing_key = Ed25519::SigningKey.generate
    verify_key = signing_key.verify_key

    allow(ENV).to receive(:fetch)
      .with('COIN_PORTAL_VERIFYING_KEY', nil)
      .and_return(verify_key.to_bytes.unpack1('H*'))

    timestamp = Time.zone.now.to_i.to_s
    message_data = request_params.merge(timestamp: timestamp)
    message = message_data.to_json
    signature = signing_key.sign(message).unpack1('H*')

    {
      'X-Signature' => signature,
      'X-Timestamp' => timestamp,
      'Content-Type' => 'application/json'
    }
  end
end
