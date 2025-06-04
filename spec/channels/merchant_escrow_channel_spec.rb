# frozen_string_literal: true

require 'rails_helper'

RSpec.describe MerchantEscrowChannel, type: :channel do
  let(:user) { create(:user, :merchant) }
  let(:merchant_escrow) { create(:merchant_escrow, user: user) }

  describe '.broadcast_to_merchant_escrow' do
    it 'calls broadcast_to with correct arguments' do
      data = { foo: 'bar' }
      expect(described_class).to receive(:broadcast_to).with(merchant_escrow, data)
      described_class.broadcast_to_merchant_escrow(merchant_escrow, data)
    end
  end

  describe '#subscribed' do
    context 'when merchant_escrow exists and belongs to user' do
      it 'streams for the merchant_escrow' do
        stub_connection current_user: user
        subscribe(escrow_id: merchant_escrow.id)
        expect(subscription).to be_confirmed
        expect(subscription).to have_stream_for(merchant_escrow)
      end
    end

    context 'when merchant_escrow does not exist' do
      it 'rejects the subscription' do
        stub_connection current_user: user
        subscribe(escrow_id: -1)
        expect(subscription).to be_rejected
      end
    end

    context 'when merchant_escrow does not belong to user' do
      it 'rejects the subscription' do
        other_user = create(:user, :merchant)
        other_escrow = create(:merchant_escrow, user: other_user)
        stub_connection current_user: user
        subscribe(escrow_id: other_escrow.id)
        expect(subscription).to be_rejected
      end
    end
  end

  describe '#unsubscribed' do
    it 'stops all streams' do
      stub_connection current_user: user
      subscribe(escrow_id: merchant_escrow.id)
      expect(subscription).to have_stream_for(merchant_escrow)
      expect(subscription).to receive(:stop_all_streams).at_least(:once)
      subscription.unsubscribe_from_channel
    end
  end

  describe '#keepalive' do
    it 'transmits keepalive_ack with timestamp' do
      stub_connection current_user: user
      subscribe(escrow_id: merchant_escrow.id)
      expect(subscription).to be_confirmed
      expect(subscription).to receive(:transmit).with(hash_including(status: 'success', message: 'keepalive_ack', timestamp: kind_of(Integer)))
      subscription.keepalive({})
    end
  end
end
