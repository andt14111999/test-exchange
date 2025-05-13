# frozen_string_literal: true

require 'rails_helper'

describe KafkaService::Services::Tick::TickService do
  describe '#query' do
    let(:service) { described_class.new }
    let(:pool_pair) { 'USDT/VND' }
    let(:payload) { { 'eventId' => 'test-123', 'operationType' => 'tick_query' } }

    it 'sends an event to the tick query topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::TICK_QUERY_TOPIC,
        key: pool_pair,
        data: payload
      )

      service.query(pool_pair: pool_pair, payload: payload)
    end
  end
end
