# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinPortalController, type: :controller do
  describe '#index' do
    describe 'authentication' do
      it 'returns unauthorized when signature is missing' do
        post :index, params: { type: 'deposit' }
        expect(response).to have_http_status(:unauthorized)
        expect(response.body).to eq('Missing signature')
      end

      it 'returns unauthorized when timestamp is missing' do
        request.headers['X-Signature'] = 'some-signature'
        post :index, params: { type: 'deposit' }
        expect(response).to have_http_status(:unauthorized)
        expect(response.body).to eq('Missing timestamp')
      end

      it 'returns unauthorized when Ed25519 verification fails' do
        request.headers['X-Signature'] = 'invalid-signature'
        request.headers['X-Timestamp'] = Time.current.to_i.to_s

        verify_key = instance_double(Ed25519::VerifyKey)
        allow(Ed25519::VerifyKey).to receive(:new).and_return(verify_key)
        allow(verify_key).to receive(:verify).and_raise(Ed25519::VerifyError)

        post :index, params: { type: 'deposit' }

        expect(response).to have_http_status(:unauthorized)
        expect(response.body).to eq('Invalid signature')
      end

      it 'returns unauthorized when signature verification raises other errors' do
        request.headers['X-Signature'] = 'invalid-signature'
        request.headers['X-Timestamp'] = Time.current.to_i.to_s

        verify_key = instance_double(Ed25519::VerifyKey)
        allow(Ed25519::VerifyKey).to receive(:new).and_return(verify_key)
        allow(verify_key).to receive(:verify).and_raise(StandardError)

        post :index, params: { type: 'deposit' }

        expect(response).to have_http_status(:unauthorized)
        expect(response.body).to eq('Authentication error')
      end

      it 'successfully verifies valid signature' do
        timestamp = Time.current.to_i.to_s
        request.headers['X-Signature'] = 'valid-signature'
        request.headers['X-Timestamp'] = timestamp

        verify_key = instance_double(Ed25519::VerifyKey)
        allow(Ed25519::VerifyKey).to receive(:new).and_return(verify_key)

        # Only address will be in request_parameters because type is a route parameter
        request_params = { address: 'non-existent-address' }
        # Use a matcher that ignores key order in JSON
        expect(verify_key).to receive(:verify) do |sig, msg|
          expect(sig).to eq([ 'valid-signature' ].pack('H*'))
          expect(JSON.parse(msg)).to eq(JSON.parse(request_params.merge(timestamp: timestamp).to_json))
          true
        end

        post :index, params: request_params.merge(type: 'deposit')

        # Since the account doesn't exist, we expect a bad request response
        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to eq('Account not found')
      end

      it 'returns internal server error when verifying key is missing' do
        allow(ENV).to receive(:fetch).with('COIN_PORTAL_VERIFYING_KEY', nil).and_return(nil)
        request.headers['X-Signature'] = 'some-signature'
        request.headers['X-Timestamp'] = Time.current.to_i.to_s
        post :index, params: { type: 'deposit' }
        expect(response).to have_http_status(:internal_server_error)
        expect(response.body).to eq('Server configuration error')
      end
    end

    describe 'type validation' do
      before do
        allow_any_instance_of(described_class).to receive(:authenticate_request).and_return(true)
      end

      it 'returns bad request for unsupported type' do
        post :index, params: { type: 'unsupported' }
        expect(response).to have_http_status(:bad_request)
        expect(response.body).to eq('unsupported type')
      end
    end

    describe 'deposit handling' do
      let(:user) { create(:user) }
      let(:coin_account) do
        create(:coin_account,
          user: user,
          coin_currency: 'usdt',
          layer: 'trc20',
          account_type: 'deposit',
          address: 'TRX123'
        )
      end

      let(:deposit_params) do
        {
          type: 'deposit',
          coin: 'trct',
          address: coin_account.address,
          amount: '100.0',
          tx_hash: 'tx123',
          out_index: 0,
          confirmations_count: 1,
          required_confirmations_count: 12
        }
      end

      before do
        allow_any_instance_of(described_class).to receive(:authenticate_request).and_return(true)
        stub_const('CoinAccount::PORTAL_COIN_TO_COIN_CURRENCY', { 'trct' => 'usdt' })
      end

      it 'handles successful deposit' do
        allow_any_instance_of(User).to receive(:active?).and_return(true)
        allow_any_instance_of(CoinAccount).to receive(:handle_deposit).and_return([
          instance_double(CoinDeposit, persisted?: true),
          true
        ])

        post :index, params: deposit_params

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('created_at')
      end

      it 'returns bad request when account is not found' do
        post :index, params: deposit_params.merge(address: 'invalid-address')

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to eq('Account not found')
      end

      it 'returns forbidden when user account is not active' do
        allow_any_instance_of(User).to receive(:active?).and_return(false)

        post :index, params: deposit_params

        expect(response).to have_http_status(:forbidden)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to eq('Account is not active')
      end

      it 'returns bad request when deposit fails' do
        allow_any_instance_of(User).to receive(:active?).and_return(true)
        allow_any_instance_of(CoinAccount).to receive(:handle_deposit).and_return([ 'Invalid amount', false ])

        post :index, params: deposit_params

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to eq('Invalid amount')
      end

      it 'returns internal server error when unexpected error occurs' do
        allow_any_instance_of(User).to receive(:active?).and_return(true)
        allow_any_instance_of(CoinAccount).to receive(:handle_deposit).and_raise(StandardError, 'Unexpected error')

        post :index, params: deposit_params

        expect(response).to have_http_status(:internal_server_error)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to eq('Internal server error')
      end
    end
  end
end
