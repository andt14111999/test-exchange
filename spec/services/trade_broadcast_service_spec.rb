require 'rails_helper'

RSpec.describe TradeBroadcastService do
  let(:trade) { create(:trade) }

  describe '.call' do
    it 'calls instance method' do
      service = instance_double(described_class)
      allow(described_class).to receive(:new).with(trade).and_return(service)
      allow(service).to receive(:call)

      described_class.call(trade)
      expect(service).to have_received(:call)
    end
  end

  describe '#call' do
    it 'broadcasts trade data successfully' do
      expect(TradeChannel).to receive(:broadcast_to_trade).with(trade, kind_of(Hash))
      allow(trade).to receive(:respond_to?).and_call_original
      allow(trade).to receive(:respond_to?).with(:delivered).and_return(true)
      allow(trade).to receive(:update).with(delivered: true)

      expect(described_class.new(trade).call).to be true
    end

    it 'handles broadcast failure' do
      allow(TradeChannel).to receive(:broadcast_to_trade).and_raise(StandardError)
      allow(Rails.logger).to receive(:error)
      allow(trade).to receive(:respond_to?).and_call_original
      allow(trade).to receive(:respond_to?).with(:delivered).and_return(true)
      allow(trade).to receive(:update).with(delivered: false)

      expect(described_class.new(trade).call).to be false
      expect(Rails.logger).to have_received(:error).with(/Error broadcasting trade:/)
    end

    it 'returns true when trade does not respond to delivered' do
      trade_without_delivered = create(:trade)
      allow(trade_without_delivered).to receive(:respond_to?).and_call_original
      allow(trade_without_delivered).to receive(:respond_to?).with(:delivered).and_return(false)
      expect(TradeChannel).to receive(:broadcast_to_trade).with(trade_without_delivered, kind_of(Hash))

      expect(described_class.new(trade_without_delivered).call).to be true
    end
  end

  describe '#trade_data' do
    it 'returns correct trade data structure' do
      service = described_class.new(trade)
      data = service.send(:trade_data)

      expect(data).to eq(
        status: 'success',
        data: {
          id: trade.id,
          ref: trade.ref,
          status: trade.status,
          coin_currency: trade.coin_currency,
          fiat_currency: trade.fiat_currency,
          coin_amount: trade.coin_amount,
          fiat_amount: trade.fiat_amount,
          price: trade.price,
          payment_method: trade.payment_method,
          taker_side: trade.taker_side,
          buyer_id: trade.buyer_id,
          seller_id: trade.seller_id,
          created_at: trade.created_at,
          updated_at: trade.updated_at,
          paid_at: trade.paid_at,
          released_at: trade.released_at,
          cancelled_at: trade.cancelled_at,
          disputed_at: trade.disputed_at,
          expired_at: trade.expired_at
        }
      )
    end
  end
end
