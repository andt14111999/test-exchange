# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::CoinAccounts::Api, type: :request do
  describe 'GET /api/v1/coin_accounts/address' do
    context 'when user is authenticated' do
      it 'returns address for existing coin account' do
        user = create(:user)
        coin_account = create(:coin_account,
          user: user,
          coin_currency: 'eth',
          layer: 'erc20',
          account_type: 'deposit',
          address: '0x123abc'
        )
        token = JsonWebToken.encode(user_id: user.id)

        get '/api/v1/coin_accounts/address',
          params: { coin_currency: 'eth', layer: 'erc20' },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response).to eq(
          'status' => 'success',
          'data' => {
            'coin_currency' => coin_account.coin_currency,
            'layer' => coin_account.layer,
            'address' => coin_account.address
          }
        )
      end

      it 'returns 404 when coin account is not found' do
        user = create(:user)
        token = JsonWebToken.encode(user_id: user.id)

        get '/api/v1/coin_accounts/address',
          params: { coin_currency: 'nonexistent', layer: 'erc20' },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:not_found)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'Coin account not found'
        )
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/coin_accounts/address',
          params: { coin_currency: 'eth', layer: 'erc20' }

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end

  describe 'POST /api/v1/coin_accounts/generate_address' do
    context 'when user is authenticated' do
      it 'generates new address for base coin and token accounts' do
        user = create(:user)
        token = JsonWebToken.encode(user_id: user.id)
        base_coin = 'eth'
        generated_address = '0xnewaddress123'

        allow(NetworkConfigurationService).to receive(:base_coin_for_layer)
          .with('erc20')
          .and_return(base_coin)

        allow_any_instance_of(AddressGenerationService)
          .to receive(:generate)
          .and_return(generated_address)

        # Create token accounts
        create(:coin_account,
          user: user,
          coin_currency: 'usdt',
          layer: 'erc20',
          account_type: 'deposit'
        )

        post '/api/v1/coin_accounts/generate_address',
          params: { coin_currency: 'usdt', layer: 'erc20' },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:created)
        expect(json_response).to eq(
          'status' => 'success',
          'data' => {
            'base_coin' => base_coin,
            'layer' => 'erc20',
            'address' => generated_address,
            'updated_accounts' => 2 # base account + 1 token account
          }
        )

        # Verify all accounts have the new address
        user.coin_accounts.where(layer: 'erc20', account_type: 'deposit').each do |account|
          expect(account.reload.address).to eq(generated_address)
        end
      end

      it 'returns error when address generation fails' do
        user = create(:user)
        token = JsonWebToken.encode(user_id: user.id)

        allow(NetworkConfigurationService).to receive(:base_coin_for_layer)
          .with('erc20')
          .and_return('eth')

        allow_any_instance_of(AddressGenerationService)
          .to receive(:generate)
          .and_return(nil)

        post '/api/v1/coin_accounts/generate_address',
          params: { coin_currency: 'usdt', layer: 'erc20' },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'Failed to generate address'
        )
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/coin_accounts/generate_address',
          params: { coin_currency: 'usdt', layer: 'erc20' }

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end
end
