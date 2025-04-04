# rubocop:disable RSpec/VerifiedDoubles
require 'rails_helper'
require 'kafka'

module Kafka
  class Consumer; end
  class FetchedMessage; end
end

RSpec.describe KafkaService::Base::Consumer, type: :service do
  let(:group_id) { 'test_group' }
  let(:topics) { [ 'test_topic' ] }
  let(:kafka_class) { class_double(Kafka).as_stubbed_const }
  let(:kafka) { double('Kafka') }
  let(:consumer) { double('Kafka::Consumer') }
  let(:logger) { double('Logger') }

  before do
    allow(Logger).to receive(:new).and_return(logger)
    allow(kafka_class).to receive(:new).and_return(kafka)
    allow(kafka).to receive(:consumer).and_return(consumer)
    allow(consumer).to receive(:subscribe)
    allow(consumer).to receive(:stop)
    allow(consumer).to receive(:each_message)
    allow(logger).to receive(:info)
    allow(logger).to receive(:error)
  end

  describe '#initialize' do
    it 'initializes with correct configuration' do
      described_class.new(group_id: group_id, topics: topics)

      expect(kafka_class).to have_received(:new).with(
        seed_brokers: KafkaService::Config::Brokers::BROKERS,
        client_id: "#{Rails.env}_#{group_id}",
        logger: logger,
        socket_timeout: 20,
        connect_timeout: 20
      )

      expect(kafka).to have_received(:consumer).with(
        group_id: group_id,
        offset_commit_interval: 5,
        offset_commit_threshold: 100,
        offset_retention_time: 7_200,
        fetcher_max_queue_size: 100,
        session_timeout: 30,
        heartbeat_interval: 10
      )

      expect(consumer).to have_received(:subscribe).with('test_topic', start_from_beginning: false)
      expect(logger).to have_received(:info).with('Subscribed to topics: test_topic')
    end
  end

  describe '#start' do
    let(:message) { double('Kafka::FetchedMessage', topic: 'test_topic', value: '{"key": "value"}') }
    let(:consumer_instance) { described_class.new(group_id: group_id, topics: topics) }

    before do
      allow(consumer).to receive(:each_message).and_yield(message)
    end

    it 'processes messages correctly' do
      processed = false
      consumer_instance.start do |topic, payload|
        expect(topic).to eq('test_topic')
        expect(payload).to eq({ 'key' => 'value' })
        processed = true
      end

      expect(processed).to be true
      expect(logger).to have_received(:info).with('Starting to consume messages...')
    end

    it 'handles JSON parse error' do
      allow(JSON).to receive(:parse).and_raise(JSON::ParserError.new('Invalid JSON'))
      allow(message).to receive(:value).and_return('invalid json')

      consumer_instance.start { |_, _| }

      expect(logger).to have_received(:error).with('Failed to parse message: Invalid JSON')
    end

    it 'handles processing error' do
      allow(JSON).to receive(:parse).and_raise(StandardError.new('Processing error'))

      consumer_instance.start { |_, _| }

      expect(logger).to have_received(:error).with('Error processing message: Processing error')
    end

    it 'stops on consumer error' do
      allow(consumer).to receive(:each_message).and_raise(StandardError.new('Consumer error'))

      expect { consumer_instance.start { |_, _| } }.to raise_error(StandardError)
      expect(consumer).to have_received(:stop)
      expect(logger).to have_received(:error).with('Consumer error: Consumer error')
    end
  end

  describe '#stop' do
    it 'stops the consumer' do
      consumer_instance = described_class.new(group_id: group_id, topics: topics)
      consumer_instance.stop

      expect(consumer).to have_received(:stop)
      expect(logger).to have_received(:info).with('Kafka Consumer stopped')
    end
  end
end
