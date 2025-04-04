# rubocop:disable RSpec/VerifiedDoubles
require 'rails_helper'
require 'kafka'

RSpec.describe KafkaService::Base::Producer, type: :service do
  let(:kafka_class) { class_double(Kafka).as_stubbed_const }
  let(:kafka) { double('Kafka') }
  let(:producer) { double('Kafka::Producer') }
  let(:logger) { instance_double(Logger).as_null_object }

  before do
    allow(Logger).to receive(:new).and_return(logger)
    allow(kafka_class).to receive(:new).and_return(kafka)
    allow(kafka).to receive(:producer).and_return(producer)
    allow(producer).to receive(:produce)
    allow(producer).to receive(:deliver_messages)
    allow(producer).to receive(:shutdown)
    # Mock Kafka::DeliveryFailed
    stub_const('Kafka::DeliveryFailed', StandardError)
  end

  describe '#initialize' do
    it 'initializes with correct configuration' do
      described_class.new

      expect(kafka_class).to have_received(:new).with(
        KafkaService::Config::Brokers::BROKERS,
        client_id: "base_portal_#{Rails.env}",
        logger: logger,
        socket_timeout: 20,
        connect_timeout: 20
      )

      expect(kafka).to have_received(:producer).with(**KafkaService::Config::Config::PRODUCER_CONFIG)
      expect(logger).to have_received(:info).with("Kafka Producer initialized with brokers: #{KafkaService::Config::Brokers::BROKERS}")
    end
  end

  describe '#send_message' do
    let(:producer_instance) { described_class.new }
    let(:topic) { 'test_topic' }
    let(:key) { 'test_key' }
    let(:payload) { { data: 'test' } }

    it 'sends message successfully' do
      producer_instance.send_message(topic: topic, key: key, payload: payload)

      expect(producer).to have_received(:produce).with(payload.to_json, topic: topic, key: key)
      expect(producer).to have_received(:deliver_messages)
    end

    it 'raises error when message delivery fails' do
      allow(producer).to receive(:deliver_messages).and_raise(StandardError.new('Delivery failed'))

      expect { producer_instance.send_message(topic: topic, key: key, payload: payload) }
        .to raise_error(StandardError)
      expect(logger).to have_received(:error).with('Failed to send message: Delivery failed')
    end
  end

  describe '#send_messages_batch' do
    let(:producer_instance) { described_class.new }
    let(:messages) do
      [
        { topic: 'topic1', key: 'key1', payload: { data: '1' } },
        { topic: 'topic2', key: 'key2', payload: { data: '2' } }
      ]
    end

    it 'sends messages in batches successfully' do
      producer_instance.send_messages_batch(messages, 1)

      messages.each do |msg|
        expect(producer).to have_received(:produce).with(msg[:payload].to_json, topic: msg[:topic], key: msg[:key])
      end
      expect(producer).to have_received(:deliver_messages).twice
      expect(logger).to have_received(:info).with('Sending 2 messages in 2 batches')
      expect(logger).to have_received(:info).with('Batch 1/2 sent successfully')
      expect(logger).to have_received(:info).with('Batch 2/2 sent successfully')
    end

    context 'when delivery fails' do
      let(:delivery_error) { Kafka::DeliveryFailed.new('Failed') }

      it 'retries with smaller batch size on delivery failure' do
        messages_sent = 0
        retry_attempted = false

        # First attempt fails, second attempt succeeds
        allow(producer).to receive(:deliver_messages) do
          messages_sent += 1
          if messages_sent == 1
            raise delivery_error
          end
        end

        # Allow all info messages
        allow(logger).to receive(:info)

        # Track the sequence of error and retry
        expect(logger).to receive(:error).with('Failed to deliver batch 1: Failed') do
          expect(retry_attempted).to be false
          retry_attempted = true
        end

        producer_instance.send_messages_batch(messages, 200)

        # Verify retry was attempted
        expect(retry_attempted).to be true
        expect(messages_sent).to eq 2
      end

      it 'raises error when delivery fails with small batch size' do
        # Allow initialization message
        allow(logger).to receive(:info).with(anything)

        # Set up logger expectations in order
        expect(logger).to receive(:info).with('Sending 2 messages in 2 batches').ordered
        expect(logger).to receive(:error).with('Failed to deliver batch 1: Failed').ordered
        expect(logger).to receive(:error).with('Could not deliver messages even with small batch size').ordered

        # Always fail delivery
        allow(producer).to receive(:deliver_messages).and_raise(delivery_error)

        # Fire the test - should raise StandardError
        expect { producer_instance.send_messages_batch(messages, 1) }.to raise_error(StandardError)
      end
    end
  end

  describe '#close' do
    it 'shuts down the producer' do
      producer_instance = described_class.new
      producer_instance.close

      expect(producer).to have_received(:shutdown)
      expect(logger).to have_received(:info).with('Kafka Producer closed')
    end
  end
end
