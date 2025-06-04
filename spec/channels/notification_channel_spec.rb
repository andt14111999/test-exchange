require 'rails_helper'

RSpec.describe NotificationChannel, type: :channel do
  include ActiveSupport::Testing::TimeHelpers

  let(:user) { create(:user) }

  describe '#subscribed' do
    context 'when user is authenticated' do
      it 'successfully subscribes' do
        stub_connection current_user: user
        subscribe

        expect(subscription).to be_confirmed
        expect(subscription).to have_stream_from("notification:#{user.to_gid_param}")
      end
    end

    context 'when user is not authenticated' do
      it 'rejects subscription' do
        stub_connection current_user: nil
        subscribe

        expect(subscription).to be_rejected
      end
    end
  end

  describe '#unsubscribed' do
    it 'stops all streams' do
      stub_connection current_user: user
      subscribe

      expect { unsubscribe }.not_to raise_error
    end
  end

  describe '#keepalive' do
    before do
      stub_connection current_user: user
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
