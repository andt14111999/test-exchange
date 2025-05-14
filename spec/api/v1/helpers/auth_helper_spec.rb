# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Helpers::AuthHelper do
  def build_helper(headers)
    klass = Class.new do
      include V1::Helpers::AuthHelper
      define_method(:headers) { headers }
      define_method(:env) { { 'PATH_INFO' => '/api/v1/endpoint', 'REQUEST_METHOD' => 'GET' } }
      def error!(*); raise StandardError; end
    end
    klass.new
  end

  describe '#authenticate_user!' do
    context 'with API key authentication' do
      it 'authenticates user with valid API key' do
        user = create(:user)
        api_key = create(:api_key, user: user)
        timestamp = Time.current.to_i.to_s
        path = '/api/v1/endpoint'
        method = 'GET'
        message = "#{method}#{path}#{timestamp}"

        # Generate HMAC signature
        digest = OpenSSL::Digest.new('sha256')
        signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

        headers = {
          'X-Access-Key' => api_key.access_key,
          'X-Signature' => signature,
          'X-Timestamp' => timestamp
        }
        helper = build_helper(headers)
        helper.authenticate_user!
        expect(helper.current_user).to eq(user)
        expect(api_key.reload.last_used_at).to be_present
      end

      context 'with invalid API key' do
        it 'raises error' do
          user = create(:user)
          api_key = create(:api_key, user: user)
          timestamp = Time.current.to_i.to_s
          path = '/api/v1/endpoint'
          method = 'GET'
          message = "#{method}#{path}#{timestamp}"

          # Generate HMAC signature
          digest = OpenSSL::Digest.new('sha256')
          signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

          headers = {
            'X-Access-Key' => 'invalid_key',
            'X-Signature' => signature,
            'X-Timestamp' => timestamp
          }
          helper = build_helper(headers)
          expect { helper.authenticate_user! }.to raise_error(StandardError)
        end
      end

      context 'with invalid signature' do
        it 'raises error' do
          user = create(:user)
          api_key = create(:api_key, user: user)
          timestamp = Time.current.to_i.to_s

          headers = {
            'X-Access-Key' => api_key.access_key,
            'X-Signature' => 'invalid_signature',
            'X-Timestamp' => timestamp
          }
          helper = build_helper(headers)
          expect { helper.authenticate_user! }.to raise_error(StandardError)
        end
      end

      context 'with revoked API key' do
        it 'raises error' do
          user = create(:user)
          api_key = create(:api_key, user: user)
          api_key.update!(revoked_at: Time.current)

          timestamp = Time.current.to_i.to_s
          path = '/api/v1/endpoint'
          method = 'GET'
          message = "#{method}#{path}#{timestamp}"

          # Generate HMAC signature
          digest = OpenSSL::Digest.new('sha256')
          signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

          headers = {
            'X-Access-Key' => api_key.access_key,
            'X-Signature' => signature,
            'X-Timestamp' => timestamp
          }
          helper = build_helper(headers)
          expect { helper.authenticate_user! }.to raise_error(StandardError)
        end
      end
    end

    context 'with JWT authentication' do
      it 'authenticates user with valid JWT token' do
        user = create(:user)
        secret = Rails.application.secret_key_base
        token = JWT.encode({ user_id: user.id }, secret, 'HS256')
        headers = { 'Authorization' => "Bearer #{token}" }
        helper = build_helper(headers)
        helper.authenticate_user!
        expect(helper.current_user).to eq(user)
      end

      context 'with invalid token' do
        it 'raises error' do
          headers = { 'Authorization' => 'Bearer invalid_token' }
          helper = build_helper(headers)
          expect { helper.authenticate_user! }.to raise_error(StandardError)
        end
      end

      context 'with non-existent user' do
        it 'raises error' do
          secret = Rails.application.secret_key_base
          token = JWT.encode({ user_id: 999999 }, secret, 'HS256')
          headers = { 'Authorization' => "Bearer #{token}" }
          helper = build_helper(headers)
          expect { helper.authenticate_user! }.to raise_error(StandardError)
        end
      end

      context 'with missing token' do
        it 'raises error' do
          headers = {}
          helper = build_helper(headers)
          expect { helper.authenticate_user! }.to raise_error(StandardError)
        end
      end
    end

    context 'with no authentication' do
      it 'raises error' do
        headers = {}
        helper = build_helper(headers)
        expect { helper.authenticate_user! }.to raise_error(StandardError)
      end
    end
  end

  describe '#api_key_auth?' do
    context 'with all required headers' do
      it 'returns true' do
        headers = {
          'X-Access-Key' => 'key',
          'X-Signature' => 'signature',
          'X-Timestamp' => 'timestamp'
        }
        helper = build_helper(headers)
        expect(helper.send(:api_key_auth?)).to be true
      end
    end

    context 'with missing headers' do
      it 'returns false' do
        headers = {
          'X-Access-Key' => 'key',
          'X-Signature' => 'signature'
        }
        helper = build_helper(headers)
        expect(helper.send(:api_key_auth?)).to be false
      end
    end

    context 'with empty headers' do
      it 'returns false' do
        headers = {}
        helper = build_helper(headers)
        expect(helper.send(:api_key_auth?)).to be false
      end
    end
  end
end
