require 'rails_helper'

RSpec.describe TradeChannel, type: :channel do
  include ActiveSupport::Testing::TimeHelpers

  let(:user) { create(:user) }
  let(:trade) { create(:trade, buyer: user, seller: user) }

  describe '#subscribed' do
    it 'successfully subscribes when user is buyer or seller' do
      stub_connection current_user: user
      subscribe trade_id: trade.id
      expect(subscription).to be_confirmed
      expect(subscription).to have_stream_from("trade:#{trade.to_gid_param}")
    end

    it 'rejects subscription when trade does not exist' do
      stub_connection current_user: user
      subscribe trade_id: 0
      expect(subscription).to be_rejected
    end

    it 'rejects subscription when user is neither buyer nor seller' do
      other_user = create(:user)
      stub_connection current_user: other_user
      subscribe trade_id: trade.id
      expect(subscription).to be_rejected
    end
  end

  describe '#unsubscribed' do
    it 'stops all streams' do
      stub_connection current_user: user
      subscribe trade_id: trade.id
      expect { unsubscribe }.not_to raise_error
    end
  end

  describe '#keepalive' do
    it 'responds with success message and timestamp' do
      stub_connection current_user: user
      subscribe trade_id: trade.id
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
