# rubocop:disable RSpec/VerifiedDoubles
require 'rails_helper'

RSpec.describe KafkaService::Base::Consumer, type: :service do
  let(:group_id) { 'test_group' }
  let(:topics) { [ 'test_topic' ] }
  let(:consumer) { double('Rdkafka::Consumer') }
  let(:logger) { double('Logger') }

  before do
    allow(Logger).to receive(:new).and_return(logger)

    # Mock the Rdkafka::Config.new call to return a double that responds to consumer
    allow(Rdkafka::Config).to receive(:new) do |config|
      config_double = double('Rdkafka::Config')
      allow(config_double).to receive(:consumer).and_return(consumer)
      config_double
    end

    allow(consumer).to receive(:subscribe)
    allow(consumer).to receive(:close)
    allow(consumer).to receive(:each)
    allow(logger).to receive(:info)
    allow(logger).to receive(:error)
  end

  describe '#initialize' do
    it 'initializes with correct configuration' do
      expected_config = {
        'bootstrap.servers': KafkaService::Config::Brokers::BROKERS.join(','),
        'group.id': group_id,
        'client.id': "#{Rails.env}_#{group_id}",
        'auto.offset.reset': 'latest',
        'enable.auto.commit': true,
        'auto.commit.interval.ms': 5000,
        'session.timeout.ms': 30000,
        'heartbeat.interval.ms': 10000
      }

      described_class.new(group_id: group_id, topics: topics)

      expect(Rdkafka::Config).to have_received(:new).with(expected_config)
      expect(logger).to have_received(:info).with("RdKafka Consumer initialized for group: #{group_id}, topics: test_topic")
    end
  end

  describe '#start' do
    let(:message) { double('Rdkafka::Consumer::Message', topic: 'test_topic', payload: '{"key": "value"}') }
    let(:consumer_instance) { described_class.new(group_id: group_id, topics: topics) }

    before do
      allow(consumer).to receive(:each).and_yield(message)
    end

    it 'processes messages correctly' do
      processed = false
      consumer_instance.start do |topic, payload|
        expect(topic).to eq('test_topic')
        expect(payload).to eq({ 'key' => 'value' })
        processed = true
      end

      expect(processed).to be true
      expect(consumer).to have_received(:subscribe).with(*topics)
      expect(logger).to have_received(:info).with('Subscribed to topics: test_topic')
      expect(logger).to have_received(:info).with('Starting to consume messages...')
    end

    it 'handles JSON parse error' do
      allow(JSON).to receive(:parse).and_raise(JSON::ParserError.new('Invalid JSON'))
      allow(message).to receive(:payload).and_return('invalid json')

      consumer_instance.start { |_, _| }

      expect(logger).to have_received(:error).with('Failed to parse message: Invalid JSON')
    end

    it 'handles processing error' do
      allow(JSON).to receive(:parse).and_raise(StandardError.new('Processing error'))

      consumer_instance.start { |_, _| }

      expect(logger).to have_received(:error).with('Error processing message: Processing error')
    end

    it 'stops on consumer error' do
      allow(consumer).to receive(:each).and_raise(StandardError.new('Consumer error'))

      expect { consumer_instance.start { |_, _| } }.to raise_error(StandardError)
      expect(consumer).to have_received(:close)
      expect(logger).to have_received(:error).with('Consumer error: Consumer error')
    end
  end

  describe '#stop' do
    it 'stops the consumer' do
      consumer_instance = described_class.new(group_id: group_id, topics: topics)
      consumer_instance.stop

      expect(consumer).to have_received(:close)
      expect(logger).to have_received(:info).with('RdKafka Consumer stopped')
    end
  end
end
