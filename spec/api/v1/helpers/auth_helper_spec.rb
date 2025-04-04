require 'rails_helper'

RSpec.describe V1::Helpers::AuthHelper do
  # Create a test class that includes the helper module
  let(:test_api_class) do
    Class.new do
      include V1::Helpers::AuthHelper

      attr_accessor :headers, :current_user

      def initialize
        @headers = {}
      end

      def error!(message, status)
        raise StandardError, "#{message} (#{status})"
      end
    end
  end

  let(:api) { test_api_class.new }
  let(:user) { create(:user) }
  let(:secret) { Rails.application.secret_key_base }
  let(:token) { JWT.encode({ user_id: user.id }, secret, 'HS256') }

  describe '#authenticate_user!' do
    context 'when token is missing' do
      it 'raises unauthorized error' do
        expect { api.authenticate_user! }.to raise_error(StandardError, /{:status=>"error", :message=>"Unauthorized"} \(401\)/)
      end
    end

    context 'when token is invalid' do
      before do
        api.headers['Authorization'] = 'Bearer invalid_token'
      end

      it 'raises unauthorized error' do
        expect { api.authenticate_user! }.to raise_error(StandardError, /{:status=>"error", :message=>"Unauthorized"} \(401\)/)
      end
    end

    context 'when user is not found' do
      before do
        api.headers['Authorization'] = "Bearer #{token}"
        allow(::User).to receive(:find).and_raise(ActiveRecord::RecordNotFound)
      end

      it 'raises unauthorized error' do
        expect { api.authenticate_user! }.to raise_error(StandardError, /{:status=>"error", :message=>"Unauthorized"} \(401\)/)
      end
    end

    context 'when token is valid' do
      before do
        api.headers['Authorization'] = "Bearer #{token}"
        allow(::User).to receive(:find).and_return(user)
      end

      it 'sets current_user' do
        api.authenticate_user!
        expect(api.current_user).to eq(user)
      end

      it 'does not raise error' do
        expect { api.authenticate_user! }.not_to raise_error
      end
    end
  end

  describe '#current_user' do
    it 'returns nil when not authenticated' do
      expect(api.current_user).to be_nil
    end

    it 'returns current user when authenticated' do
      api.headers['Authorization'] = "Bearer #{token}"
      allow(::User).to receive(:find).and_return(user)
      api.authenticate_user!
      expect(api.current_user).to eq(user)
    end
  end
end
