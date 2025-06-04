# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Balances::Api, type: :request do
  let(:user) { create(:user) }
  let(:auth_headers) { { 'Authorization' => "Bearer #{generate_token_for(user)}" } }

  describe 'GET /api/v1/balances' do
    before do
      # Mock JWT authentication
      allow_any_instance_of(V1::Helpers::AuthHelper).to receive(:authenticate_jwt!).and_return(true)
      allow_any_instance_of(V1::Helpers::AuthHelper).to receive(:current_user).and_return(user)

      # Mock constants (không dùng allow vì frozen)
      stub_const('CoinAccount::SUPPORTED_NETWORKS', { 'usdt' => [] })
      stub_const('FiatAccount::SUPPORTED_CURRENCIES', { 'vnd' => nil, 'php' => nil, 'ngn' => nil })
    end

    context 'when the user has balances' do
      before do
        # Create coin accounts
        usdt_account = create(:coin_account, :usdt_main, user: user, balance: 133.580245, frozen_balance: 10.123456) # available_balance = 123.456789

        # Create fiat accounts
        vnd_account = create(:fiat_account, :vnd, user: user, balance: 600000.912, frozen_balance: 100000.123) # available_balance = 500000.789
        php_account = create(:fiat_account, :php, user: user, balance: 1200.912, frozen_balance: 200.123) # available_balance = 1000.789
        ngn_account = create(:fiat_account, :ngn, user: user, balance: 6000.912, frozen_balance: 1000.123) # available_balance = 5000.789

        # Mock find calls for accounts
        coin_scope = double('CoinScope')
        allow(user.coin_accounts).to receive(:of_coin).with('usdt').and_return(coin_scope)
        allow(coin_scope).to receive(:main).and_return(usdt_account)

        allow(user.fiat_accounts).to receive(:find_by).with(currency: 'vnd').and_return(vnd_account)
        allow(user.fiat_accounts).to receive(:find_by).with(currency: 'php').and_return(php_account)
        allow(user.fiat_accounts).to receive(:find_by).with(currency: 'ngn').and_return(ngn_account)
      end

      it 'returns rounded balances according to currency decimals' do
        get '/api/v1/balances', headers: auth_headers

        expect(response).to have_http_status(:success)
        json = JSON.parse(response.body)

        # Test USDT rounding to 6 decimals
        usdt_account = json['data']['coin_accounts'].find { |a| a['coin_currency'] == 'usdt' }
        expect(usdt_account['balance'].to_f).to eq(123.456789.round(6))
        expect(usdt_account['frozen_balance'].to_f).to eq(10.123456.round(6))

        # Test VND rounding to 0 decimals
        vnd_account = json['data']['fiat_accounts'].find { |a| a['currency'] == 'vnd' }
        expect(vnd_account['balance'].to_f).to eq(500000.789.round(0))
        expect(vnd_account['frozen_balance'].to_f).to eq(100000.123.round(0))

        # Test PHP rounding to 2 decimals
        php_account = json['data']['fiat_accounts'].find { |a| a['currency'] == 'php' }
        expect(php_account['balance'].to_f).to eq(1000.789.round(2))
        expect(php_account['frozen_balance'].to_f).to eq(200.123.round(2))

        # Test NGN rounding to 2 decimals
        ngn_account = json['data']['fiat_accounts'].find { |a| a['currency'] == 'ngn' }
        expect(ngn_account['balance'].to_f).to eq(5000.789.round(2))
        expect(ngn_account['frozen_balance'].to_f).to eq(1000.123.round(2))
      end
    end

    context 'when a user has no accounts' do
      before do
        # Mock for no accounts
        coin_scope = double('CoinScope')
        allow(user.coin_accounts).to receive(:of_coin).with('usdt').and_return(coin_scope)
        allow(coin_scope).to receive(:main).and_return(nil)

        allow(user.fiat_accounts).to receive(:find_by).and_return(nil)
      end

      it 'returns empty balances with zeros' do
        get '/api/v1/balances', headers: auth_headers

        expect(response).to have_http_status(:success)
        json = JSON.parse(response.body)

        expect(json['data']['coin_accounts']).to be_an(Array)
        expect(json['data']['fiat_accounts']).to be_an(Array)

        # Check that all balances are zero
        json['data']['coin_accounts'].each do |account|
          expect(account['balance'].to_f).to eq(0)
          expect(account['frozen_balance'].to_f).to eq(0)
        end

        json['data']['fiat_accounts'].each do |account|
          expect(account['balance'].to_f).to eq(0)
          expect(account['frozen_balance'].to_f).to eq(0)
        end
      end
    end

    context 'when user is authenticated' do
      before do
        usdt_account = create(:coin_account, :usdt_main, user: user, balance: 90.0, frozen_balance: 10.0) # available_balance = 80.0
        vnd_account = create(:fiat_account, :vnd, user: user, balance: 900000.0, frozen_balance: 100000.0) # available_balance = 800000.0
        php_account = create(:fiat_account, :php, user: user, balance: 0.0, frozen_balance: 0.0)
        ngn_account = create(:fiat_account, :ngn, user: user, balance: 0.0, frozen_balance: 0.0)

        coin_scope = double('CoinScope')
        allow(user.coin_accounts).to receive(:of_coin).with('usdt').and_return(coin_scope)
        allow(coin_scope).to receive(:main).and_return(usdt_account)

        allow(user.fiat_accounts).to receive(:find_by).with(currency: 'vnd').and_return(vnd_account)
        allow(user.fiat_accounts).to receive(:find_by).with(currency: 'php').and_return(php_account)
        allow(user.fiat_accounts).to receive(:find_by).with(currency: 'ngn').and_return(ngn_account)
      end

      it 'returns correct balances for user with coin accounts' do
        get '/api/v1/balances', headers: auth_headers

        expect(response).to have_http_status(:success)
        json = JSON.parse(response.body)

        usdt_balance = json['data']['coin_accounts'].find { |a| a['coin_currency'] == 'usdt' }
        expect(usdt_balance['balance'].to_f).to eq(80.0)
        expect(usdt_balance['frozen_balance'].to_f).to eq(10.0)
      end

      it 'returns correct balances for user with fiat accounts' do
        get '/api/v1/balances', headers: auth_headers

        expect(response).to have_http_status(:success)
        json = JSON.parse(response.body)

        vnd_balance = json['data']['fiat_accounts'].find { |a| a['currency'] == 'vnd' }
        expect(vnd_balance['balance'].to_f).to eq(800000.0)
        expect(vnd_balance['frozen_balance'].to_f).to eq(100000.0)
      end

      it 'returns correct balances for user with both coin and fiat accounts' do
        get '/api/v1/balances', headers: auth_headers

        expect(response).to have_http_status(:success)
        json = JSON.parse(response.body)

        usdt_balance = json['data']['coin_accounts'].find { |a| a['coin_currency'] == 'usdt' }
        expect(usdt_balance['balance'].to_f).to eq(80.0)
        expect(usdt_balance['frozen_balance'].to_f).to eq(10.0)

        vnd_balance = json['data']['fiat_accounts'].find { |a| a['currency'] == 'vnd' }
        expect(vnd_balance['balance'].to_f).to eq(800000.0)
        expect(vnd_balance['frozen_balance'].to_f).to eq(100000.0)
      end
    end
  end

  private

  def generate_token_for(user)
    'mock_token'
  end
end
