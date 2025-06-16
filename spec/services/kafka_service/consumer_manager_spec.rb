# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::ConsumerManager, type: :service do
  let(:consumer) { instance_double(KafkaService::Base::Consumer) }
  let(:payload) { { 'data' => 'test' } }
  let(:kafka_event) { create(:kafka_event) }
  let(:handler) { instance_double(KafkaService::Handlers::CoinAccountHandler) }

  before do
    allow(KafkaService::Base::Consumer).to receive(:new).and_return(consumer)
    allow(consumer).to receive(:start)
    allow(consumer).to receive(:stop)
    allow(Rails.logger).to receive(:info)
    allow(Rails.logger).to receive(:error)
  end

  describe '#initialize' do
    it 'initializes with correct handlers' do
      manager = described_class.new
      expected_handlers = {
        KafkaService::Config::Topics::BALANCE_UPDATE => KafkaService::Handlers::CoinAccountHandler,
        KafkaService::Config::Topics::TRANSACTION_RESULT => KafkaService::Handlers::CoinDepositHandler,
        KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC => KafkaService::Handlers::AmmPoolHandler,
        KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE => KafkaService::Handlers::MerchantEscrowHandler,
        KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC => KafkaService::Handlers::AmmPositionHandler,
        KafkaService::Config::Topics::OFFER_UPDATE => KafkaService::Handlers::OfferHandler,
        KafkaService::Config::Topics::TRADE_UPDATE => KafkaService::Handlers::TradeHandler,
        KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC => KafkaService::Handlers::AmmOrderHandler,
        KafkaService::Config::Topics::TICK_UPDATE_TOPIC => KafkaService::Handlers::TickHandler,
        KafkaService::Config::Topics::BALANCES_LOCK_UPDATE => KafkaService::Handlers::BalanceLockHandler,
        KafkaService::Config::Topics::TRANSACTION_RESPONSE => KafkaService::Handlers::TransactionResponseHandler
      }
      actual_handlers = described_class.new.instance_variable_get(:@handlers)
      expect(actual_handlers.keys).to match_array(expected_handlers.keys)
      expected_handlers.each do |topic, handler_class|
        expect(actual_handlers[topic]).to be_a(handler_class)
      end
    end
  end

  describe '#start' do
    it 'starts consumers for each topic' do
      manager = described_class.new
      expect(KafkaService::Base::Consumer).to receive(:new).at_least(:once).and_return(consumer)
      manager.start
    end
  end

  describe '#stop' do
    it 'stops all consumers' do
      manager = described_class.new
      manager.start
      expect(consumer).to receive(:stop).at_least(:once)
      manager.stop
    end
  end

  describe '#process_message' do
    let(:manager) { described_class.new }
    let(:topic) { KafkaService::Config::Topics::BALANCE_UPDATE }
    let(:event_id) { 'test_event_id' }
    let(:payload) { { 'messageId' => event_id, 'data' => 'test' } }

    context 'when event is valid' do
      it 'processes message successfully' do
        # Mock KafkaEvent.where to return an empty relation/array
        kafka_event_relation = double('ActiveRecord::Relation', exists?: false)
        allow(KafkaEvent).to receive(:where).with(event_id: event_id, topic_name: topic).and_return(kafka_event_relation)

        # Mock store_event to return a valid kafka_event
        allow(manager).to receive(:store_event).with(topic, event_id, payload).and_return(kafka_event)

        # Allow kafka_event.update! to be called
        allow(kafka_event).to receive(:update!)

        # Allow handler.handle to be called
        allow(handler).to receive(:handle)

        # Call the method
        manager.send(:process_message, topic, handler, payload)

        # Verify that handle was called with the payload
        expect(handler).to have_received(:handle).with(payload)
      end
    end

    context 'when event is a duplicate' do
      it 'skips processing' do
        # Mock KafkaEvent.where to return a relation that exists?
        kafka_event_relation = double('ActiveRecord::Relation', exists?: true)
        allow(KafkaEvent).to receive(:where).with(event_id: event_id, topic_name: topic).and_return(kafka_event_relation)

        allow(handler).to receive(:handle)

        manager.send(:process_message, topic, handler, payload)

        # Verify that handle was not called
        expect(handler).not_to have_received(:handle)
      end
    end

    context 'when event_id is blank' do
      it 'returns early' do
        payload_without_id = { 'data' => 'test' }
        allow(handler).to receive(:handle)

        manager.send(:process_message, topic, handler, payload_without_id)

        # Verify that handle was not called
        expect(handler).not_to have_received(:handle)
      end
    end

    context 'when payload is a string' do
      it 'parses JSON payload' do
        string_payload = payload.to_json

        # Mock KafkaEvent.where to return an empty relation/array
        kafka_event_relation = double('ActiveRecord::Relation', exists?: false)
        allow(KafkaEvent).to receive(:where).with(event_id: event_id, topic_name: topic).and_return(kafka_event_relation)

        # Mock store_event to return a valid kafka_event
        allow(manager).to receive(:store_event).with(topic, event_id, payload).and_return(kafka_event)

        # Allow kafka_event.update! to be called
        allow(kafka_event).to receive(:update!)

        # Allow handler.handle to be called
        allow(handler).to receive(:handle)

        # Call the method
        manager.send(:process_message, topic, handler, string_payload)

        # Verify that handle was called with the parsed payload
        expect(handler).to have_received(:handle).with(payload)
      end
    end

    context 'when JSON parsing fails' do
      it 'logs error and returns' do
        invalid_json = 'invalid json'
        allow(handler).to receive(:handle)

        manager.send(:process_message, topic, handler, invalid_json)

        # Verify that handle was not called
        expect(handler).not_to have_received(:handle)
      end
    end

    context 'when handler raises error' do
      it 'logs error and continues' do
        # Mock KafkaEvent.where to return an empty relation/array
        kafka_event_relation = double('ActiveRecord::Relation', exists?: false)
        allow(KafkaEvent).to receive(:where).with(event_id: event_id, topic_name: topic).and_return(kafka_event_relation)

        # Mock store_event to return a valid kafka_event
        allow(manager).to receive(:store_event).with(topic, event_id, payload).and_return(kafka_event)

        allow(handler).to receive(:handle).and_raise(StandardError.new('handler error'))
        allow(kafka_event).to receive(:update!)
        expect { manager.send(:process_message, topic, handler, payload) }.not_to raise_error
      end
    end
  end

  describe '#store_event' do
    let(:manager) { described_class.new }
    let(:topic) { KafkaService::Config::Topics::BALANCE_UPDATE }
    let(:event_id) { 'test_event_id' }
    let(:payload) { { 'data' => 'test' } }

    context 'when event creation succeeds' do
      it 'creates and returns event' do
        expect(KafkaEvent).to receive(:create!).with(
          event_id: event_id,
          topic_name: topic,
          payload: payload,
          status: 'received'
        ).and_return(kafka_event)
        expect(manager.send(:store_event, topic, event_id, payload)).to eq(kafka_event)
      end
    end

    context 'when event creation fails' do
      it 'logs error and returns nil' do
        allow(KafkaEvent).to receive(:create!).and_raise(StandardError.new('creation error'))
        expect(manager.send(:store_event, topic, event_id, payload)).to be_nil
      end
    end
  end

  describe '#process_message_with_retry' do
    let(:manager) { described_class.new }
    let(:topic) { KafkaService::Config::Topics::BALANCE_UPDATE }

    context 'when processing succeeds' do
      it 'processes message with retries' do
        expect(Retriable).to receive(:retriable).and_yield
        expect(manager).to receive(:process_message).with(topic, handler, payload)
        expect(ActiveRecord::Base.connection_pool).to receive(:with_connection).and_yield
        manager.send(:process_message_with_retry, topic, handler, payload)
      end
    end

    context 'when processing fails after retries' do
      it 'logs error' do
        allow(Retriable).to receive(:retriable).and_raise(StandardError.new('retry error'))
        expect(Rails.logger).to receive(:error).with('Failed to process message after retries: retry error')
        manager.send(:process_message_with_retry, topic, handler, payload)
      end
    end
  end

  describe '#restart_consumer' do
    let(:manager) { described_class.new }
    let(:topic) { KafkaService::Config::Topics::BALANCE_UPDATE }

    it 'waits and restarts consumer' do
      expect(manager).to receive(:sleep).with(5)
      expect(manager).to receive(:start_consumer_with_monitor).with(topic, handler)
      manager.send(:restart_consumer, topic, handler)
    end
  end
end
