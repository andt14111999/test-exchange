# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Users::MerchantRegistration, type: :request do
  describe 'POST /api/v1/merchant_registration' do
    context 'when user is authenticated' do
      it 'registers user as merchant successfully' do
        user = create(:user, role: 'user', status: 'active')
        token = JsonWebToken.encode(user_id: user.id)

        post '/api/v1/merchant_registration', headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:created)
        expect(json_response).to include(
          'id' => user.id,
          'email' => user.email,
          'role' => 'merchant'
        )
        expect(user.reload.role).to eq('merchant')
      end

      it 'returns error when user is already a merchant' do
        user = create(:user, role: 'merchant', status: 'active')
        token = JsonWebToken.encode(user_id: user.id)

        post '/api/v1/merchant_registration', headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'Cannot register as merchant. Please ensure you have completed KYC level 2 and your account is active.'
        )
        expect(user.reload.role).to eq('merchant')
      end

      it 'returns error when user is suspended' do
        user = create(:user, role: 'user', status: 'suspended')
        token = JsonWebToken.encode(user_id: user.id)

        post '/api/v1/merchant_registration', headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'Cannot register as merchant. Please ensure you have completed KYC level 2 and your account is active.'
        )
        expect(user.reload.role).to eq('user')
      end

      it 'returns error when user is banned' do
        user = create(:user, role: 'user', status: 'banned')
        token = JsonWebToken.encode(user_id: user.id)

        post '/api/v1/merchant_registration', headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'Cannot register as merchant. Please ensure you have completed KYC level 2 and your account is active.'
        )
        expect(user.reload.role).to eq('user')
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/merchant_registration'

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end
end
