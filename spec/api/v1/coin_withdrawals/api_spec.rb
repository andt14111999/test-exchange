# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::CoinWithdrawals::Api, type: :request do
  let(:user) { create(:user) }
  let(:other_user) { create(:user) }
  let(:coin_currency) { 'usdt' }
  let(:coin_layer) { 'erc20' }
  let(:coin_amount) { 50.0 }
  let(:coin_address) { '0x1234567890abcdef1234567890abcdef12345678' }
  let(:main_coin_account) { create(:coin_account, :usdt_main, user: user, balance: 200.0) }
  let(:deposit_coin_account) { create(:coin_account, :usdt_erc20, user: user) }
  let(:other_user_main_account) { create(:coin_account, :usdt_main, user: other_user, balance: 200.0) }
  let(:auth_header) { auth_headers(user) }
  let(:valid_params) do
    {
      coin_address: coin_address,
      coin_amount: coin_amount,
      coin_currency: coin_currency,
      coin_layer: coin_layer
    }
  end

  before do
    # Ensure the coin accounts exist
    main_coin_account
    deposit_coin_account
    other_user_main_account
  end

  describe 'POST /api/v1/coin_withdrawals' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/coin_withdrawals', params: valid_params
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'with valid parameters' do
        it 'creates a new withdrawal' do
          expect {
            post '/api/v1/coin_withdrawals', params: valid_params, headers: auth_header
          }.to change(CoinWithdrawal, :count).by(1)

          expect(response).to have_http_status(:success)

          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
          expect(json_response['data']['coin_currency']).to eq(coin_currency)
          expect(json_response['data']['coin_amount'].to_f).to eq(coin_amount)
          expect(json_response['data']['coin_address']).to eq(coin_address)
          expect(json_response['data']['coin_layer']).to eq(coin_layer)
          expect(json_response['data']['status']).to eq('pending')
        end
      end

      context 'with insufficient balance' do
        let(:coin_amount) { 300.0 }

        it 'returns a validation error' do
          post '/api/v1/coin_withdrawals', params: valid_params, headers: auth_header

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('error')
          expect(json_response['message']).to include('Coin amount exceeds available balance')
        end
      end
    end
  end

  describe 'GET /api/v1/coin_withdrawals/:id' do
    let!(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: coin_currency, coin_amount: coin_amount, coin_address: coin_address, coin_layer: coin_layer) }
    let!(:other_user_withdrawal) { create(:coin_withdrawal, user: other_user, coin_currency: coin_currency, coin_amount: coin_amount, coin_address: coin_address, coin_layer: coin_layer) }

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get "/api/v1/coin_withdrawals/#{withdrawal.id}"
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'when withdrawal exists and belongs to user' do
        it 'returns the withdrawal details' do
          get "/api/v1/coin_withdrawals/#{withdrawal.id}", headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
          expect(json_response['data']['id']).to eq(withdrawal.id)
          expect(json_response['data']['coin_currency']).to eq(withdrawal.coin_currency)
          expect(json_response['data']['coin_amount']).to eq(withdrawal.coin_amount.to_s)
          expect(json_response['data']['coin_fee']).to eq(withdrawal.coin_fee.to_s)
          expect(json_response['data']['coin_address']).to eq(withdrawal.coin_address)
          expect(json_response['data']['coin_layer']).to eq(withdrawal.coin_layer)
          expect(json_response['data']['status']).to eq(withdrawal.status)
        end
      end

      context 'when withdrawal does not exist' do
        it 'returns not found error' do
          get "/api/v1/coin_withdrawals/999999", headers: auth_header

          expect(response).to have_http_status(:not_found)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('error')
          expect(json_response['message']).to eq('Coin withdrawal not found')
        end
      end

      context 'when withdrawal belongs to another user' do
        it 'returns not found error' do
          get "/api/v1/coin_withdrawals/#{other_user_withdrawal.id}", headers: auth_header

          expect(response).to have_http_status(:not_found)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('error')
          expect(json_response['message']).to eq('Coin withdrawal not found')
        end
      end
    end
  end
end
