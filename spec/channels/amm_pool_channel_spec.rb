# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AmmPoolChannel, type: :channel do
  include ActiveSupport::Testing::TimeHelpers

  describe '.channel_name' do
    it 'returns the underscored name of the class' do
      expect(described_class.channel_name).to eq('amm_pool_channel')
    end
  end

  describe '#subscribed' do
    it 'successfully subscribes to the channel' do
      stub_connection
      subscribe

      expect(subscription).to be_confirmed
      expect(subscription.streams).to include("amm_pool_channel:amm_pool_channel")
    end
  end

  describe '#unsubscribed' do
    it 'stops all streams' do
      stub_connection
      subscribe

      expect { unsubscribe }.not_to raise_error
    end
  end

  describe '#keepalive' do
    before do
      stub_connection
      subscribe
    end

    it 'responds with success message and timestamp' do
      travel_to Time.zone.local(2024, 1, 1, 12, 0, 0) do
        perform :keepalive

        expect(transmissions.last).to eq(
          'status' => 'success',
          'message' => 'keepalive_ack',
          'timestamp' => Time.current.to_i
        )
      end
    end
  end
end
