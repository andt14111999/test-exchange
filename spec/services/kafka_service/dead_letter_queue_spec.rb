# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::DeadLetterQueue, type: :service do
  include ActiveSupport::Testing::TimeHelpers

  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:topic) { 'test.topic' }
  let(:payload) { { 'identifier' => '123', 'data' => 'test' } }
  let(:error) { StandardError.new('test error') }
  let(:frozen_time) { Time.zone.local(2024, 4, 4, 12, 0, 0) }

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)
    allow(producer).to receive(:close)
    travel_to frozen_time
  end

  after { travel_back }

  describe '.publish' do
    it 'publishes message to DLQ topic with identifier' do
      expect(producer).to receive(:send_message).with(
        hash_including(
          topic: "#{topic}.dlq",
          key: payload['identifier'],
          payload: {
            original_message: payload,
            error: {
              message: error.message,
              backtrace: error.backtrace,
              timestamp: frozen_time
            }
          }
        )
      )

      described_class.publish(topic: topic, payload: payload, error: error)
    end

    it 'publishes message to DLQ topic with generated UUID when no identifier' do
      payload_without_id = { 'data' => 'test' }
      expect(SecureRandom).to receive(:uuid).and_return('generated-uuid')

      expect(producer).to receive(:send_message).with(
        hash_including(
          topic: "#{topic}.dlq",
          key: 'generated-uuid',
          payload: {
            original_message: payload_without_id,
            error: {
              message: error.message,
              backtrace: error.backtrace,
              timestamp: frozen_time
            }
          }
        )
      )

      described_class.publish(topic: topic, payload: payload_without_id, error: error)
    end

    it 'closes producer after publishing' do
      described_class.publish(topic: topic, payload: payload, error: error)
      expect(producer).to have_received(:close)
    end

    it 'closes producer even when error occurs during publishing' do
      allow(producer).to receive(:send_message).and_raise(StandardError.new('publish error'))

      expect { described_class.publish(topic: topic, payload: payload, error: error) }.to raise_error(StandardError)
      expect(producer).to have_received(:close)
    end

    it 'includes error backtrace in payload' do
      error_with_backtrace = StandardError.new('test error')
      error_with_backtrace.set_backtrace([ 'line1', 'line2', 'line3', 'line4', 'line5', 'line6' ])

      expect(producer).to receive(:send_message).with(
        hash_including(
          topic: "#{topic}.dlq",
          key: payload['identifier'],
          payload: {
            original_message: payload,
            error: {
              message: error_with_backtrace.message,
              backtrace: [ 'line1', 'line2', 'line3', 'line4', 'line5' ],
              timestamp: frozen_time
            }
          }
        )
      )

      described_class.publish(topic: topic, payload: payload, error: error_with_backtrace)
    end
  end
end
