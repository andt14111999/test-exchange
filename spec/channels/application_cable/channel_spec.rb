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

  describe '#connect' do
    context 'with authenticated user' do
      it 'successfully connects and sets current_user' do
        warden = instance_double(Warden::Proxy, user: user)
        allow(connection).to receive(:env).and_return({ 'warden' => warden })

        subscribe
        # Test connect method directly
        subscription.connect

        # Verify current_user is set and accessible
        expect(subscription.current_user).to eq(user)
        expect(subscription).to be_confirmed
      end

      it 'finds verified user from warden' do
        warden = instance_double(Warden::Proxy, user: user)
        allow(connection).to receive(:env).and_return({ 'warden' => warden })
        subscribe

        # Test find_verified_user directly
        expect(subscription.send(:find_verified_user)).to eq(user)
      end
    end

    context 'without authenticated user' do
      it 'rejects connection when warden user is nil' do
        warden = instance_double(Warden::Proxy, user: nil)
        allow(connection).to receive(:env).and_return({ 'warden' => warden })
        subscribe

        expect { subscription.send(:find_verified_user) }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end

      it 'rejects connection when warden is not present' do
        allow(connection).to receive(:env).and_return({})
        subscribe

        expect { subscription.send(:find_verified_user) }.to raise_error(
          ActionCable::Connection::Authorization::UnauthorizedError
        )
      end
    end
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
end
