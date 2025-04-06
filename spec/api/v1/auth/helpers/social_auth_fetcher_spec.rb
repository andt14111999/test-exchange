# rubocop:disable RSpec/VerifiedDoubles
require 'rails_helper'

RSpec.describe V1::Auth::Helpers::SocialAuthFetcher do
  let(:test_class) do
    Class.new do
      include Grape::API::Helpers
      include V1::Auth::Helpers::SocialAuthFetcher
      attr_accessor :params

      def error!(message, status = 400)
        raise Grape::Exceptions::Base.new(message: message, status: status)
      end
    end
  end

  let(:social_auth_fetcher) { test_class.new }

  describe '#fetch_facebook_auth' do
    let(:facebook_profile) do
      {
        'id' => '123456',
        'name' => 'Facebook User',
        'email' => 'facebook@example.com',
        'picture' => {
          'data' => {
            'url' => 'http://example.com/facebook.jpg'
          }
        }
      }
    end

    before do
      allow(Koala::Facebook::API).to receive(:new).and_return(double(get_object: facebook_profile))
      social_auth_fetcher.params = { access_token: 'facebook_token' }
    end

    it 'returns auth hash with correct data' do
      result = social_auth_fetcher.fetch_facebook_auth

      expect(result.provider).to eq('facebook')
      expect(result.uid).to eq('123456')
      expect(result.info.email).to eq('facebook@example.com')
      expect(result.info.name).to eq('Facebook User')
      expect(result.info.image).to eq('http://example.com/facebook.jpg')
      expect(result.credentials.token).to eq('facebook_token')
      expect(result.credentials.expires_at).to be_within(1.second).of(60.days.from_now.to_i)
      expect(result.extra.raw_info).to eq(facebook_profile)
    end
  end

  describe '#fetch_google_auth' do
    let(:google_payload) do
      {
        'sub' => '123456',
        'email' => 'google@example.com',
        'name' => 'Google User',
        'picture' => 'http://example.com/google.jpg',
        'exp' => 1.week.from_now.to_i
      }
    end

    before do
      allow(GoogleIDToken::Validator).to receive(:new).and_return(double(check: google_payload))
      social_auth_fetcher.params = { id_token: 'google_token' }
      allow(ENV).to receive(:fetch).with('GOOGLE_CLIENT_ID', nil).and_return('google_client_id')
    end

    it 'returns auth hash with correct data' do
      result = social_auth_fetcher.fetch_google_auth

      expect(result.provider).to eq('google')
      expect(result.uid).to eq('123456')
      expect(result.info.email).to eq('google@example.com')
      expect(result.info.name).to eq('Google User')
      expect(result.info.image).to eq('http://example.com/google.jpg')
      expect(result.credentials.token).to eq('google_token')
      expect(result.credentials.expires_at).to be_within(1.second).of(Time.zone.at(google_payload['exp']))
      expect(result.extra.raw_info).to eq(google_payload)
    end
  end

  describe '#fetch_apple_auth' do
    let(:apple_token_data) do
      OpenStruct.new(
        sub: '123456',
        email: 'apple@example.com',
        exp: 1.week.from_now.to_i,
        to_h: { 'some' => 'data' }
      )
    end

    before do
      stub_const('AppleAuth', double)
      allow(AppleAuth).to receive(:verify_identity_token).and_return(apple_token_data)
      social_auth_fetcher.params = {
        identity_token: 'apple_token',
        user: { name: 'Apple User' }
      }
    end

    it 'returns auth hash with correct data' do
      result = social_auth_fetcher.fetch_apple_auth

      expect(result.provider).to eq('apple')
      expect(result.uid).to eq('123456')
      expect(result.info.email).to eq('apple@example.com')
      expect(result.info.name).to eq('Apple User')
      expect(result.info.image).to be_nil
      expect(result.credentials.token).to eq('apple_token')
      expect(result.credentials.expires_at).to eq(apple_token_data.exp)
      expect(result.extra.raw_info).to eq(apple_token_data.to_h)
    end

    it 'raises error with invalid token' do
      allow(AppleAuth).to receive(:verify_identity_token).and_raise(JWT::DecodeError)
      expect { social_auth_fetcher.fetch_apple_auth }.to raise_error(Grape::Exceptions::Base) do |error|
        expect(error.message).to eq('Invalid Apple identity token')
        expect(error.status).to eq(422)
      end
    end
  end
end
