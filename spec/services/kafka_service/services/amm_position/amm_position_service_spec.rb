# frozen_string_literal: true

require 'rails_helper'

describe KafkaService::Services::AmmPosition::AmmPositionService do
  describe '#create' do
    let(:service) { described_class.new }
    let(:identifier) { 'amm_position_123_usdt/vnd_1650000000' }
    let(:payload) { { 'eventId' => 'test-123', 'operationType' => 'amm_position_create' } }

    it 'sends an event to the amm position topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::AMM_POSITION_TOPIC,
        key: identifier,
        data: payload
      )

      service.create(identifier: identifier, payload: payload)
    end
  end

  describe '#collect_fee' do
    let(:service) { described_class.new }
    let(:identifier) { 'amm_position_123_usdt/vnd_1650000000' }
    let(:payload) { { 'eventId' => 'test-123', 'operationType' => 'amm_position_collect_fee' } }

    it 'sends an event to the amm position topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::AMM_POSITION_TOPIC,
        key: identifier,
        data: payload
      )

      service.collect_fee(identifier: identifier, payload: payload)
    end
  end

  describe '#close' do
    let(:service) { described_class.new }
    let(:identifier) { 'amm_position_123_usdt/vnd_1650000000' }
    let(:payload) { { 'eventId' => 'test-123', 'operationType' => 'amm_position_close' } }

    it 'sends an event to the amm position topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::AMM_POSITION_TOPIC,
        key: identifier,
        data: payload
      )

      service.close(identifier: identifier, payload: payload)
    end
  end
end
