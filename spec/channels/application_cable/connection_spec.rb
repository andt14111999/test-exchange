require 'rails_helper'

RSpec.describe ApplicationCable::Connection, type: :channel do
  let(:user) { create(:user) }

  describe '#connect' do
    context 'with invalid JWT token' do
      it 'rejects connection' do
        # Create a connection instance with proper initialization
        env = Rack::MockRequest.env_for('/cable', params: { 'token' => 'invalid-token' })

        connection = described_class.new(
          ActionCable::Server::Base.new,
          env
        )

        # Override internal connection methods
        allow(connection).to receive(:transmit)

        # Mock request.params to return our token
        request = instance_double(ActionDispatch::Request)
        allow(request).to receive_messages(env: env, params: { 'token' => 'invalid-token' }, headers: {})
        allow(connection).to receive_messages(logger: Logger.new(nil), request: request)

        # Mock JWT.decode to raise an error
        allow(JWT).to receive(:decode).and_raise(JWT::DecodeError)

        # Expect an unauthorized error to be raised during connection
        expect { connection.connect }.to raise_error(ActionCable::Connection::Authorization::UnauthorizedError)
      end
    end

    context 'with valid JWT token but nonexistent user' do
      let(:token) { 'valid-token-nonexistent-user' }

      it 'rejects connection' do
        # Create a connection instance with proper initialization
        env = Rack::MockRequest.env_for('/cable', params: { 'token' => token })

        connection = described_class.new(
          ActionCable::Server::Base.new,
          env
        )

        # Override internal connection methods
        allow(connection).to receive(:transmit)

        # Mock request.params to return our token
        request = instance_double(ActionDispatch::Request)
        allow(request).to receive_messages(env: env, params: { 'token' => token }, headers: {})
        allow(connection).to receive_messages(logger: Logger.new(nil), request: request)

        # Mock JWT.decode to return a valid payload but with nonexistent user
        allow(JWT).to receive(:decode).with(
          token,
          Rails.application.secret_key_base,
          true,
          { algorithm: 'HS256' }
        ).and_return([ { 'user_id' => 9999 } ])

        # Setup User.find_by to return nil for nonexistent user
        allow(User).to receive(:find_by).with(id: 9999).and_return(nil)

        # Expect an unauthorized error to be raised during connection
        expect { connection.connect }.to raise_error(ActionCable::Connection::Authorization::UnauthorizedError)
      end
    end

    context 'without authentication' do
      it 'rejects connection' do
        # Create a connection instance with proper initialization
        env = Rack::MockRequest.env_for('/cable')

        connection = described_class.new(
          ActionCable::Server::Base.new,
          env
        )

        # Override internal connection methods
        allow(connection).to receive(:transmit)

        # Mock request.params to return an empty hash
        request = instance_double(ActionDispatch::Request)
        allow(request).to receive_messages(env: env, params: {}, headers: {})
        allow(connection).to receive_messages(logger: Logger.new(nil), request: request)

        # Expect an unauthorized error to be raised during connection
        expect { connection.connect }.to raise_error(ActionCable::Connection::Authorization::UnauthorizedError)
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
end
