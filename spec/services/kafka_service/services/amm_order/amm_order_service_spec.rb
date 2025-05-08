# frozen_string_literal: true

require 'rails_helper'

describe KafkaService::Services::AmmOrder::AmmOrderService do
  let(:service) { described_class.new }
  let(:identifier) { 'test_order_123' }
  let(:payload) do
    {
      eventId: 'test-event-id',
      operationType: 'amm_order_create',
      identifier: identifier
    }
  end

  describe '#create' do
    it 'sends the event to the proper topic' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::AMM_ORDER_TOPIC,
        key: identifier,
        data: payload
      )

      service.create(identifier: identifier, payload: payload)
    end
  end
end
