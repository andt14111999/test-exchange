# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Base::Service, type: :service do
  let(:logger) { instance_double(Logger, error: nil, info: nil) }
  let(:producer_logger) { instance_double(Logger, error: nil, info: nil) }
  let(:producer) { instance_double(KafkaService::Base::Producer) }

  before do
    allow(Logger).to receive(:new).and_return(logger)
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
  end

  describe '#initialize' do
    it 'initializes logger and producer' do
      expect(Logger).to receive(:new).with('log/service.log').and_return(logger)
      expect(KafkaService::Base::Producer).to receive(:new)

      described_class.new
    end

    it 'raises error when producer initialization fails' do
      allow(KafkaService::Base::Producer).to receive(:new).and_raise(StandardError.new('Producer error'))

      expect { described_class.new }.to raise_error(StandardError, 'Producer error')
    end
  end

  describe '#service_name' do
    it 'returns underscored class name' do
      service = described_class.new
      expect(service.send(:service_name)).to eq('service')
    end
  end

  describe 'event handling' do
    before do
      stub_const('KafkaService::TestService', Class.new(described_class) do
        def test_send_event(topic:, key:, data:)
          send_event(topic: topic, key: key, data: data)
        end
      end)

      allow(SecureRandom).to receive(:uuid).and_return(uuid)
      allow(Time).to receive(:current).and_return(current_time)
    end

    let(:service) { KafkaService::TestService.new }
    let(:topic) { 'test_topic' }
    let(:key) { 'test_key' }
    let(:data) { { test: 'data' } }
    let(:uuid) { 'test-uuid' }
    let(:current_time) { Time.current }

    it 'sends event with default data' do
      expect(logger).to receive(:info).with("Sending event to #{topic}")
      expect(producer).to receive(:send_message).with(
        topic: topic,
        key: key,
        payload: {
          test: 'data',
          eventId: uuid,
          timestamp: current_time.to_i * 1000
        }
      )

      service.test_send_event(topic: topic, key: key, data: data)
    end

    it 'logs error and reraises when sending fails' do
      error = StandardError.new('Send error')
      allow(producer).to receive(:send_message).and_raise(error)

      expect(logger).to receive(:error).with('test_service error: Send error')

      expect do
        service.test_send_event(topic: topic, key: key, data: data)
      end.to raise_error(StandardError, 'Send error')
    end
  end
end
