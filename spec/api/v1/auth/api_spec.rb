# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Auth::Api, type: :request do
  let(:auth_helper) { V1::Auth::Helpers::SocialAuthFetcher }
  let(:user) { create(:user, email: 'user@example.com', display_name: 'Test User') }
  let(:jwt_token) { 'jwt-token-123' }

  before do
    allow_any_instance_of(V1::Auth::Helpers::JwtHelper).to receive(:generate_jwt_token).and_return(jwt_token)
  end

  describe 'POST /api/v1/auth/facebook' do
    let(:access_token) { 'valid_facebook_token' }
    let(:auth_hash) do
      AuthHash.new(
        provider: 'facebook',
        uid: '123456',
        info: {
          email: 'user@example.com',
          name: 'Facebook User',
          image: 'https://graph.facebook.com/123456/picture'
        },
        credentials: {
          token: access_token,
          expires_at: 1.day.from_now
        },
        extra: { 'id' => '123456' }
      )
    end

    before do
      allow_any_instance_of(auth_helper).to receive(:fetch_facebook_auth).and_return(auth_hash)
    end

    it 'authenticates user with valid Facebook token' do
      social_account = create(:social_account, user: user, provider: 'facebook', provider_user_id: '123456')
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account).and_return(social_account)

      post '/api/v1/auth/facebook', params: { access_token: access_token }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => user.email,
        'display_name' => user.display_name
      )
    end

    it 'creates new user when social account does not exist' do
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account) do |_instance, auth|
        new_user = create(:user, email: auth.info.email, display_name: auth.info.name)
        create(:social_account, user: new_user, provider: auth.provider, provider_user_id: auth.uid)
      end

      post '/api/v1/auth/facebook', params: { access_token: access_token }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => 'user@example.com',
        'display_name' => 'Facebook User'
      )
    end

    it 'creates merchant account when account_type is merchant' do
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account) do |_instance, auth|
        new_user = create(:user, email: auth.info.email, display_name: auth.info.name, role: 'merchant')
        create(:social_account, user: new_user, provider: auth.provider, provider_user_id: auth.uid)
      end

      post '/api/v1/auth/facebook', params: { access_token: access_token, account_type: 'merchant' }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => 'user@example.com',
        'display_name' => 'Facebook User',
        'role' => 'merchant'
      )
    end

    it 'returns error with invalid token' do
      allow_any_instance_of(auth_helper).to receive(:fetch_facebook_auth)
        .and_raise(StandardError.new('Invalid token'))

      post '/api/v1/auth/facebook', params: { access_token: 'invalid_token' }

      expect(response).to have_http_status(:unprocessable_entity)
      expect(json_response['error']).to eq('Invalid token')
    end
  end

  describe 'POST /api/v1/auth/google' do
    let(:id_token) { 'valid_google_token' }
    let(:auth_hash) do
      AuthHash.new(
        provider: 'google',
        uid: '123456',
        info: {
          email: 'user@example.com',
          name: 'Google User',
          image: 'https://lh3.googleusercontent.com/photo.jpg'
        },
        credentials: {
          token: id_token,
          expires_at: 1.day.from_now
        },
        extra: { 'sub' => '123456' }
      )
    end

    before do
      allow_any_instance_of(auth_helper).to receive(:fetch_google_auth).and_return(auth_hash)
    end

    it 'authenticates user with valid Google token' do
      social_account = create(:social_account, user: user, provider: 'google', provider_user_id: '123456')
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account).and_return(social_account)

      post '/api/v1/auth/google', params: { id_token: id_token }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => user.email,
        'display_name' => user.display_name
      )
    end

    it 'creates new user when social account does not exist' do
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account) do |_instance, auth|
        new_user = create(:user, email: auth.info.email, display_name: auth.info.name)
        create(:social_account, user: new_user, provider: auth.provider, provider_user_id: auth.uid)
      end

      post '/api/v1/auth/google', params: { id_token: id_token }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => 'user@example.com',
        'display_name' => 'Google User'
      )
    end

    it 'creates merchant account when account_type is merchant' do
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account) do |_instance, auth|
        new_user = create(:user, email: auth.info.email, display_name: auth.info.name, role: 'merchant')
        create(:social_account, user: new_user, provider: auth.provider, provider_user_id: auth.uid)
      end

      post '/api/v1/auth/google', params: { id_token: id_token, account_type: 'merchant' }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => 'user@example.com',
        'display_name' => 'Google User',
        'role' => 'merchant'
      )
    end

    it 'returns error with invalid token' do
      allow_any_instance_of(auth_helper).to receive(:fetch_google_auth)
        .and_raise(StandardError.new('Invalid token'))

      post '/api/v1/auth/google', params: { id_token: 'invalid_token' }

      expect(response).to have_http_status(:unprocessable_entity)
      expect(json_response['error']).to eq('Invalid token')
    end
  end

  describe 'POST /api/v1/auth/apple' do
    let(:identity_token) { 'valid_apple_token' }
    let(:auth_hash) do
      AuthHash.new(
        provider: 'apple',
        uid: '123456',
        info: {
          email: 'user@example.com',
          name: 'Apple User',
          image: nil
        },
        credentials: {
          token: identity_token,
          expires_at: 1.day.from_now
        },
        extra: { 'sub' => '123456' }
      )
    end

    before do
      allow_any_instance_of(auth_helper).to receive(:fetch_apple_auth).and_return(auth_hash)
    end

    it 'authenticates user with valid Apple token' do
      social_account = create(:social_account, user: user, provider: 'apple', provider_user_id: '123456')
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account).and_return(social_account)

      post '/api/v1/auth/apple', params: {
        identity_token: identity_token,
        user: { name: 'Apple User' }
      }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => user.email,
        'display_name' => user.display_name
      )
    end

    it 'creates new user when social account does not exist' do
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account) do |_instance, auth|
        new_user = create(:user, email: auth.info.email, display_name: auth.info.name)
        create(:social_account, user: new_user, provider: auth.provider, provider_user_id: auth.uid)
      end

      post '/api/v1/auth/apple', params: {
        identity_token: identity_token,
        user: { name: 'Apple User' }
      }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => 'user@example.com',
        'display_name' => 'Apple User'
      )
    end

    it 'creates merchant account when account_type is merchant' do
      allow_any_instance_of(V1::Auth::Helpers::SocialAccountHandler).to receive(:find_or_create_social_account) do |_instance, auth|
        new_user = create(:user, email: auth.info.email, display_name: auth.info.name, role: 'merchant')
        create(:social_account, user: new_user, provider: auth.provider, provider_user_id: auth.uid)
      end

      post '/api/v1/auth/apple', params: {
        identity_token: identity_token,
        user: { name: 'Apple User' },
        account_type: 'merchant'
      }

      expect(response).to have_http_status(:created)
      expect(json_response['token']).to eq(jwt_token)
      expect(json_response['user']).to include(
        'email' => 'user@example.com',
        'display_name' => 'Apple User',
        'role' => 'merchant'
      )
    end

    it 'returns error with invalid token' do
      allow_any_instance_of(auth_helper).to receive(:fetch_apple_auth)
        .and_raise(StandardError.new('Invalid token'))

      post '/api/v1/auth/apple', params: {
        identity_token: 'invalid_token',
        user: { name: 'Apple User' }
      }

      expect(response).to have_http_status(:unprocessable_entity)
      expect(json_response['error']).to eq('Invalid token')
    end
  end
end
