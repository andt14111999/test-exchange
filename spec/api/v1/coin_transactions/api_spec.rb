# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::CoinTransactions::Api, type: :request do
  describe 'GET /api/v1/coin_transactions' do
    let(:user) { create(:user) }
    let(:token) { JsonWebToken.encode(user_id: user.id) }
    let(:coin_currency) { 'usdt' }
    let(:coin_account) { create(:coin_account, :main, user: user, coin_currency: coin_currency, layer: 'all', balance: 200.0) }
    let(:deposit) do
      create(:coin_deposit,
        user: user,
        coin_account: coin_account,
        coin_currency: coin_currency,
        coin_amount: 100.0,
        tx_hash: '0x123',
        status: 'verified'
      )
    end
    let(:withdrawal) do
      create(:coin_withdrawal,
        user: user,
        coin_currency: coin_currency,
        coin_amount: 50.0,
        coin_address: '0xde709f2102306220921060314715629080e2fb77',
        status: 'completed',
        tx_hash: '0x789'
      )
    end

    before do
      coin_account
    end

    context 'when user is authenticated' do
      it 'returns paginated deposits and withdrawals' do
        deposit && withdrawal
        get '/api/v1/coin_transactions',
          params: { coin_currency: coin_currency },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response).to eq(
          'status' => 'success',
          'data' => {
            'deposits' => [
              {
                'id' => deposit.id,
                'amount' => '100.0',
                'coin_currency' => coin_currency,
                'status' => 'verified',
                'hash' => '0x123',
                'created_at' => deposit.created_at.as_json,
                'updated_at' => deposit.updated_at.as_json
              }
            ],
            'withdrawals' => [
              {
                'id' => withdrawal.id,
                'amount' => '50.0',
                'coin_currency' => coin_currency,
                'status' => 'completed',
                'hash' => '0x789',
                'address' => '0xde709f2102306220921060314715629080e2fb77',
                'created_at' => withdrawal.created_at.as_json,
                'updated_at' => withdrawal.updated_at.as_json
              }
            ],
            'pagination' => {
              'deposits' => {
                'current_page' => 1,
                'total_pages' => 1,
                'total_count' => 1,
                'per_page' => 20
              },
              'withdrawals' => {
                'current_page' => 1,
                'total_pages' => 1,
                'total_count' => 1,
                'per_page' => 20
              }
            }
          }
        )
      end

      it 'respects pagination parameters' do
        deposit && withdrawal
        get '/api/v1/coin_transactions',
          params: { coin_currency: coin_currency, page: 1, per_page: 1 },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response['data']['deposits'].size).to eq(1)
        expect(json_response['data']['withdrawals'].size).to eq(1)
        expect(json_response['data']['pagination']['deposits']['per_page']).to eq(1)
        expect(json_response['data']['pagination']['withdrawals']['per_page']).to eq(1)
      end

      it 'returns empty lists when no transactions exist' do
        get '/api/v1/coin_transactions',
          params: { coin_currency: coin_currency },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response).to eq(
          'status' => 'success',
          'data' => {
            'deposits' => [],
            'withdrawals' => [],
            'pagination' => {
              'deposits' => {
                'current_page' => 1,
                'total_pages' => 0,
                'total_count' => 0,
                'per_page' => 20
              },
              'withdrawals' => {
                'current_page' => 1,
                'total_pages' => 0,
                'total_count' => 0,
                'per_page' => 20
              }
            }
          }
        )
      end

      it 'returns 404 error when no coin accounts exist for currency' do
        get '/api/v1/coin_transactions',
          params: { coin_currency: 'nonexistent' },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:not_found)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'No coin accounts found for this currency'
        )
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/coin_transactions',
          params: { coin_currency: 'usdt' }

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end
end
