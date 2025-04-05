require 'rails_helper'

describe KafkaService::Services::AmmPool::AmmPoolService do
  describe '#create' do
    let(:service) { described_class.new }
    let(:pair) { 'USDT/VND' }
    let(:payload) { { 'eventId' => 'test-123', 'operationType' => 'amm_pool_create' } }

    it 'sends an event to the amm pool topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::AMM_POOL_TOPIC,
        key: pair,
        data: payload
      )

      service.create(pair: pair, payload: payload)
    end
  end

  describe '#update' do
    let(:service) { described_class.new }
    let(:pair) { 'USDT/VND' }
    let(:payload) { { 'eventId' => 'test-123', 'operationType' => 'amm_pool_update' } }

    it 'sends an event to the amm pool topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::AMM_POOL_TOPIC,
        key: pair,
        data: payload
      )

      service.update(pair: pair, payload: payload)
    end
  end
end
