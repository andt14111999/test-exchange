# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Merchant::Escrows, type: :request do
  describe 'POST /api/v1/merchant_escrows' do
    context 'when user is not a merchant' do
      it 'returns unauthorized error' do
        user = create(:user)
        params = {
          usdt_amount: 100.0,
          fiat_currency: 'VND'
        }

        post '/api/v1/merchant_escrows', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unauthorized)
        expect(json_response['error']).to eq('Unauthorized')
      end
    end

    context 'when user is a merchant' do
      it 'creates a new escrow' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 0.0)
        params = {
          usdt_amount: 100.0,
          fiat_currency: 'VND'
        }

        post '/api/v1/merchant_escrows', params: params, headers: auth_headers(merchant)

        expect(response).to have_http_status(:success)
        expect(json_response['usdt_amount']).to eq('100.0')
        expect(json_response['fiat_currency']).to eq('VND')
        expect(json_response['status']).to eq('pending')
      end

      it 'returns validation errors when params are invalid' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 0.0)
        params = {
          usdt_amount: -100.0,
          fiat_currency: 'VND'
        }

        post '/api/v1/merchant_escrows', params: params, headers: auth_headers(merchant)

        expect(response).to have_http_status(:unprocessable_entity)
      end
    end
  end

  describe 'GET /api/v1/merchant_escrows' do
    context 'when user is not a merchant' do
      it 'returns unauthorized error' do
        user = create(:user)

        get '/api/v1/merchant_escrows', headers: auth_headers(user)

        expect(response).to have_http_status(:unauthorized)
        expect(json_response['error']).to eq('Unauthorized')
      end
    end

    context 'when user is a merchant' do
      it 'returns list of escrows' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 0.0)
        escrow = create(:merchant_escrow, user: merchant, usdt_account: usdt_account, fiat_account: fiat_account)

        get '/api/v1/merchant_escrows', headers: auth_headers(merchant)

        expect(response).to have_http_status(:success)
        expect(json_response.first['id']).to eq(escrow.id)
      end
    end
  end

  describe 'GET /api/v1/merchant_escrows/:id' do
    context 'when user is not a merchant' do
      it 'returns unauthorized error' do
        user = create(:user)
        escrow = create(:merchant_escrow)

        get "/api/v1/merchant_escrows/#{escrow.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:unauthorized)
        expect(json_response['error']).to eq('Unauthorized')
      end
    end

    context 'when user is a merchant' do
      it 'returns the escrow' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 0.0)
        escrow = create(:merchant_escrow, user: merchant, usdt_account: usdt_account, fiat_account: fiat_account)

        get "/api/v1/merchant_escrows/#{escrow.id}", headers: auth_headers(merchant)

        expect(response).to have_http_status(:success)
        expect(json_response['id']).to eq(escrow.id)
      end

      it 'returns not found when escrow does not exist' do
        merchant = create(:user, :merchant)

        get '/api/v1/merchant_escrows/0', headers: auth_headers(merchant)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Escrow not found')
      end
    end
  end

  describe 'POST /api/v1/merchant_escrows/:id/cancel' do
    context 'when user is not a merchant' do
      it 'returns unauthorized error' do
        user = create(:user)
        escrow = create(:merchant_escrow)

        post "/api/v1/merchant_escrows/#{escrow.id}/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:unauthorized)
        expect(json_response['error']).to eq('Unauthorized')
      end
    end

    context 'when user is a merchant' do
      it 'cancels the escrow' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0, frozen_balance: 100.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 5000000.0, frozen_balance: 2500000.0)
        escrow = create(:merchant_escrow, :active, user: merchant, usdt_account: usdt_account, fiat_account: fiat_account, usdt_amount: 100.0, fiat_amount: 2500000.0)

        post "/api/v1/merchant_escrows/#{escrow.id}/cancel", headers: auth_headers(merchant)

        expect(response).to have_http_status(:success)
        expect(json_response['status']).to eq('active')
      end

      it 'returns not found when escrow does not exist' do
        merchant = create(:user, :merchant)

        post '/api/v1/merchant_escrows/0/cancel', headers: auth_headers(merchant)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Escrow not found')
      end

      it 'returns error when escrow cannot be cancelled' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 0.0)
        escrow = create(:merchant_escrow, :cancelled, user: merchant, usdt_account: usdt_account, fiat_account: fiat_account)

        post "/api/v1/merchant_escrows/#{escrow.id}/cancel", headers: auth_headers(merchant)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Cannot cancel this escrow')
      end

      it 'returns error when cancel operation fails' do
        merchant = create(:user, :merchant)
        usdt_account = create(:coin_account, :main, user: merchant, coin_currency: 'usdt', balance: 1000.0, frozen_balance: 100.0)
        fiat_account = create(:fiat_account, user: merchant, currency: 'VND', balance: 2500000.0, frozen_balance: 2500000.0)
        escrow = create(:merchant_escrow, :active, user: merchant, usdt_account: usdt_account, fiat_account: fiat_account, usdt_amount: 100.0)

        allow_any_instance_of(MerchantEscrowService).to receive(:cancel).and_raise(StandardError.new('Failed to process merchant escrow operation'))

        post "/api/v1/merchant_escrows/#{escrow.id}/cancel", headers: auth_headers(merchant)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to process merchant escrow operation')
      end
    end
  end
end
