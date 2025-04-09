# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApplicationCable::Channel, type: :channel do
  let(:user) { create(:user) }
  let(:logger) { Logger.new(nil) }
  let(:connection) do
    instance_double(ApplicationCable::Connection,
      current_user: user,
      identifiers: [ :current_user ],
      logger: logger,
      transmit: nil)
  end

  before do
    stub_connection(
      current_user: user,
      identifiers: [ :current_user ],
      logger: logger,
      transmit: nil
    )
  end

  describe '#disconnect' do
    it 'stops all streams' do
      subscribe
      expect(subscription).to receive(:stop_all_streams)

      subscription.disconnect
    end
  end

  describe '#current_user' do
    before do
      subscribe
    end

    it 'returns the current user from connection' do
      expect(connection).to receive(:current_user).twice.and_return(user)

      # Call current_user multiple times to ensure coverage
      expect(subscription.current_user).to eq(user)
      expect(subscription.current_user).to eq(user)
    end

    it 'returns nil when no user is present' do
      expect(connection).to receive(:current_user).and_return(nil)

      expect(subscription.current_user).to be_nil
    end
  end

  describe '#env' do
    before do
      subscribe
    end

    it 'returns the env from connection' do
      env_hash = { 'key' => 'value' }
      expect(connection).to receive(:env).and_return(env_hash)

      expect(subscription.send(:env)).to eq(env_hash)
    end
  end

  describe '#reject_unauthorized_connection' do
    before do
      subscribe
    end

    it 'raises an UnauthorizedError' do
      expect { subscription.send(:reject_unauthorized_connection) }.to raise_error(
        ActionCable::Connection::Authorization::UnauthorizedError
      )
    end
  end
end
