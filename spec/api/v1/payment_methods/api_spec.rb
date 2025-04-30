# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::PaymentMethods::Api, type: :request do
  let(:user) { create(:user) }

  describe 'GET /api/v1/payment_methods' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        get '/api/v1/payment_methods'

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'returns all payment methods' do
        payment_method1 = create(:payment_method, country_code: 'US', enabled: true)
        payment_method2 = create(:payment_method, country_code: 'VN', enabled: false)

        get '/api/v1/payment_methods', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(2)
        expect(json_response.map { |pm| pm['id'] }).to contain_exactly(payment_method1.id, payment_method2.id)
      end

      it 'filters payment methods by country_code' do
        us_payment_method = create(:payment_method, country_code: 'US')
        vn_payment_method = create(:payment_method, country_code: 'VN')

        get '/api/v1/payment_methods', params: { country_code: 'US' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(1)
        expect(json_response[0]['id']).to eq(us_payment_method.id)
        expect(json_response[0]['country_code']).to eq('US')
      end

      it 'filters payment methods by enabled=true' do
        enabled_payment_method = create(:payment_method, enabled: true)
        disabled_payment_method = create(:payment_method, enabled: false)

        get '/api/v1/payment_methods', params: { enabled: true }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(1)
        expect(json_response[0]['id']).to eq(enabled_payment_method.id)
        expect(json_response[0]['enabled']).to eq(true)
      end

      it 'filters payment methods by enabled=false' do
        enabled_payment_method = create(:payment_method, enabled: true)
        disabled_payment_method = create(:payment_method, enabled: false)

        get '/api/v1/payment_methods', params: { enabled: false }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(1)
        expect(json_response[0]['id']).to eq(disabled_payment_method.id)
        expect(json_response[0]['enabled']).to eq(false)
      end

      it 'combines country_code and enabled filters' do
        us_enabled = create(:payment_method, country_code: 'US', enabled: true)
        us_disabled = create(:payment_method, country_code: 'US', enabled: false)
        vn_enabled = create(:payment_method, country_code: 'VN', enabled: true)

        get '/api/v1/payment_methods',
            params: { country_code: 'US', enabled: true },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(1)
        expect(json_response[0]['id']).to eq(us_enabled.id)
        expect(json_response[0]['country_code']).to eq('US')
        expect(json_response[0]['enabled']).to eq(true)
      end
    end
  end

  describe 'GET /api/v1/payment_methods/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        payment_method = create(:payment_method)

        get "/api/v1/payment_methods/#{payment_method.id}"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'returns the payment method details' do
        payment_method = create(:payment_method,
                                name: 'bank_transfer',
                                display_name: 'Bank Transfer',
                                country_code: 'US',
                                enabled: true)

        get "/api/v1/payment_methods/#{payment_method.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response['id']).to eq(payment_method.id)
        expect(json_response['name']).to eq('bank_transfer')
        expect(json_response['display_name']).to eq('Bank Transfer')
        expect(json_response['country_code']).to eq('US')
        expect(json_response['enabled']).to eq(true)
      end

      it 'returns 404 when payment method is not found' do
        get '/api/v1/payment_methods/non_existent_id', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end
end
