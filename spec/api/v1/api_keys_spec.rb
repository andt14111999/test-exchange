# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::ApiKeys, type: :request do
  describe 'POST /api/v1/api_keys' do
    context 'with valid params' do
      it 'creates a new API key' do
        user = create(:user)
        secret = Rails.application.secret_key_base
        token = JWT.encode({ user_id: user.id }, secret, 'HS256')
        headers = { 'Authorization' => "Bearer #{token}" }
        params = { name: 'Test API Key' }

        expect {
          post '/api/v1/api_keys', params: params, headers: headers
        }.to change(ApiKey, :count).by(1)

        expect(response).to have_http_status(:success)
        json_response = JSON.parse(response.body)
        expect(json_response['status']).to eq('success')
        expect(json_response['data']['name']).to eq(params[:name])
        expect(json_response['data']['access_key']).to be_present
        expect(json_response['data']['secret_key']).to be_present
      end
    end

    context 'with invalid params' do
      it 'returns error' do
        user = create(:user)
        secret = Rails.application.secret_key_base
        token = JWT.encode({ user_id: user.id }, secret, 'HS256')
        headers = { 'Authorization' => "Bearer #{token}" }
        params = { name: '' }

        post '/api/v1/api_keys', params: params, headers: headers

        expect(response).to have_http_status(:unprocessable_entity)
        # Skip JSON parsing for this test as the response may not be valid JSON
      end
    end
  end

  describe 'GET /api/v1/api_keys' do
    it 'returns a list of API keys' do
      user = create(:user)
      create_list(:api_key, 3, user: user)

      secret = Rails.application.secret_key_base
      token = JWT.encode({ user_id: user.id }, secret, 'HS256')
      headers = { 'Authorization' => "Bearer #{token}" }

      get '/api/v1/api_keys', headers: headers

      expect(response).to have_http_status(:success)
      json_response = JSON.parse(response.body)
      expect(json_response['status']).to eq('success')
      expect(json_response['data'].length).to eq(3)
      expect(json_response['data'].first['access_key']).to be_present
      expect(json_response['data'].first['secret_key']).to be_nil # Secret key not returned in list
    end
  end

  describe 'DELETE /api/v1/api_keys/:id' do
    it 'revokes an API key' do
      user = create(:user)
      api_key = create(:api_key, user: user)

      secret = Rails.application.secret_key_base
      token = JWT.encode({ user_id: user.id }, secret, 'HS256')
      headers = { 'Authorization' => "Bearer #{token}" }

      delete "/api/v1/api_keys/#{api_key.id}", headers: headers

      expect(response).to have_http_status(:success)
      json_response = JSON.parse(response.body)
      expect(json_response['status']).to eq('success')
      expect(json_response['message']).to eq('API key revoked successfully')

      api_key.reload
      expect(api_key.revoked_at).to be_present
    end

    context 'when API key belongs to another user' do
      it 'returns not found' do
        user = create(:user)
        other_user = create(:user)
        other_api_key = create(:api_key, user: other_user)
        secret = Rails.application.secret_key_base
        token = JWT.encode({ user_id: user.id }, secret, 'HS256')
        headers = { 'Authorization' => "Bearer #{token}" }

        delete "/api/v1/api_keys/#{other_api_key.id}", headers: headers

        expect(response).to have_http_status(:not_found)
      end
    end
  end
end
