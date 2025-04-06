# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Balances::Api, type: :request do
  def json_response
    JSON.parse(response.body)
  end

  describe 'GET /api/v1/balances' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/balances'

        expect(response).to have_http_status(:unauthorized)
        expect(json_response['status']).to eq('error')
        expect(json_response['message']).to eq('Unauthorized')
      end
    end

    context 'when user is authenticated' do
      let(:user) { create(:user) }

      it 'returns empty balances for user with no accounts' do
        get '/api/v1/balances', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        expect(json_response['status']).to eq('success')
        expect(json_response['data']['coin_accounts']).to be_present
        expect(json_response['data']['fiat_accounts']).to be_present
      end

      it 'returns correct balances for user with coin accounts' do
        create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 100.0, frozen_balance: 20.0)
        create(:coin_account, :main, user: user, coin_currency: 'btc', balance: 1.0, frozen_balance: 0.5)

        get '/api/v1/balances', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        expect(json_response['status']).to eq('success')

        usdt_balance = json_response['data']['coin_accounts'].find { |acc| acc['coin_currency'] == 'usdt' }
        expect(usdt_balance['balance'].to_f).to eq(100.0)
        expect(usdt_balance['frozen_balance'].to_f).to eq(20.0)

        btc_balance = json_response['data']['coin_accounts'].find { |acc| acc['coin_currency'] == 'btc' }
        expect(btc_balance['balance'].to_f).to eq(1.0)
        expect(btc_balance['frozen_balance'].to_f).to eq(0.5)
      end

      it 'returns correct balances for user with fiat accounts' do
        create(:fiat_account, user: user, currency: 'VND', balance: 1000000.0, frozen_balance: 200000.0)
        create(:fiat_account, user: user, currency: 'PHP', balance: 50000.0, frozen_balance: 10000.0)

        get '/api/v1/balances', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        expect(json_response['status']).to eq('success')

        vnd_balance = json_response['data']['fiat_accounts'].find { |acc| acc['currency'] == 'VND' }
        expect(vnd_balance['balance'].to_f).to eq(1000000.0)
        expect(vnd_balance['frozen_balance'].to_f).to eq(200000.0)

        php_balance = json_response['data']['fiat_accounts'].find { |acc| acc['currency'] == 'PHP' }
        expect(php_balance['balance'].to_f).to eq(50000.0)
        expect(php_balance['frozen_balance'].to_f).to eq(10000.0)
      end

      it 'returns correct balances for user with both coin and fiat accounts' do
        create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 100.0, frozen_balance: 20.0)
        create(:fiat_account, user: user, currency: 'VND', balance: 1000000.0, frozen_balance: 200000.0)

        get '/api/v1/balances', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        expect(json_response['status']).to eq('success')

        usdt_balance = json_response['data']['coin_accounts'].find { |acc| acc['coin_currency'] == 'usdt' }
        expect(usdt_balance['balance'].to_f).to eq(100.0)
        expect(usdt_balance['frozen_balance'].to_f).to eq(20.0)

        vnd_balance = json_response['data']['fiat_accounts'].find { |acc| acc['currency'] == 'VND' }
        expect(vnd_balance['balance'].to_f).to eq(1000000.0)
        expect(vnd_balance['frozen_balance'].to_f).to eq(200000.0)
      end

      it 'returns zero balances correctly' do
        create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 0.0, frozen_balance: 0.0)
        create(:fiat_account, user: user, currency: 'VND', balance: 0.0, frozen_balance: 0.0)

        get '/api/v1/balances', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        expect(json_response['status']).to eq('success')

        usdt_balance = json_response['data']['coin_accounts'].find { |acc| acc['coin_currency'] == 'usdt' }
        expect(usdt_balance['balance'].to_f).to eq(0.0)
        expect(usdt_balance['frozen_balance'].to_f).to eq(0.0)

        vnd_balance = json_response['data']['fiat_accounts'].find { |acc| acc['currency'] == 'VND' }
        expect(vnd_balance['balance'].to_f).to eq(0.0)
        expect(vnd_balance['frozen_balance'].to_f).to eq(0.0)
      end
    end
  end
end
