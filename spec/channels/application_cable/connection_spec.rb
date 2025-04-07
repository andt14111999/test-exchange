# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApplicationCable::Connection, type: :channel do
  let(:user) { create(:user) }
  let(:secret_key_base) { 'test_secret_key_base' }

  before do
    allow(Rails.application).to receive(:credentials).and_return(
      ActiveSupport::InheritableOptions.new(secret_key_base: secret_key_base)
    )
  end

  describe '#connect' do
    context 'with Devise session' do
      it 'connects when user is authenticated via Devise' do
        # Create a connection with warden user
        warden = instance_double(Warden::Proxy, user: user)
        env = { 'warden' => warden }
        connection = build_connection(env)

        connection.connect

        expect(connection.current_user).to eq(user)
      end
    end

    context 'with JWT token in params' do
      it 'connects when valid token is provided in params' do
        token = generate_token(user.id)
        env = Rack::MockRequest.env_for('/cable', params: { 'token' => token })
        connection = build_connection(env)

        connection.connect

        expect(connection.current_user).to eq(user)
      end

      it 'rejects when invalid token is provided in params' do
        env = Rack::MockRequest.env_for('/cable', params: { 'token' => 'invalid-token' })
        connection = build_connection(env)

        expect { connection.connect }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end

      it 'rejects when JWT raises StandardError' do
        allow(JWT).to receive(:decode).and_raise(StandardError.new('Some error'))
        token = generate_token(user.id)
        env = Rack::MockRequest.env_for('/cable', params: { 'token' => token })
        connection = build_connection(env)

        expect { connection.connect }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end
    end

    context 'with JWT token in headers' do
      it 'connects when valid token is provided in Authorization header' do
        token = generate_token(user.id)
        env = Rack::MockRequest.env_for(
          '/cable',
          'HTTP_AUTHORIZATION' => "Bearer #{token}"
        )
        connection = build_connection(env)

        connection.connect

        expect(connection.current_user).to eq(user)
      end

      it 'rejects when invalid token is provided in Authorization header' do
        env = Rack::MockRequest.env_for(
          '/cable',
          'HTTP_AUTHORIZATION' => 'Bearer invalid-token'
        )
        connection = build_connection(env)

        expect { connection.connect }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end
    end

    context 'with invalid user' do
      it 'rejects when user does not exist' do
        token = generate_token(0)
        env = Rack::MockRequest.env_for('/cable', params: { 'token' => token })
        connection = build_connection(env)

        expect { connection.connect }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end
    end

    context 'without authentication' do
      it 'rejects when no authentication is provided' do
        env = Rack::MockRequest.env_for('/cable')
        connection = build_connection(env)

        expect { connection.connect }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end
    end
  end

  describe '#disconnect' do
    it 'handles disconnection gracefully' do
      # Create a connection instance with proper initialization
      env = Rack::MockRequest.env_for('/cable')

      connection = described_class.new(
        ActionCable::Server::Base.new,
        env
      )

      # Override internal connection methods
      allow(connection).to receive(:transmit)

      # Mock request object
      request = instance_double(ActionDispatch::Request)
      allow(request).to receive_messages(env: env, params: {}, headers: {})
      allow(connection).to receive_messages(logger: Logger.new(nil), request: request)

      # Disconnect should not raise any errors
      expect { connection.disconnect }.not_to raise_error
    end
  end

  private

  def build_connection(env)
    connection = described_class.new(
      ActionCable::Server::Base.new,
      env
    )

    # Mock request object
    request = instance_double(ActionDispatch::Request,
      env: env,
      params: env['QUERY_STRING'] ? Rack::Utils.parse_query(env['QUERY_STRING']) : {},
      headers: env
    )

    # Mock logger to prevent actual logging
    allow(connection).to receive_messages(logger: Logger.new(nil), request: request)

    connection
  end

  def generate_token(user_id)
    JWT.encode(
      { user_id: user_id },
      secret_key_base,
      'HS256'
    )
  end
end
