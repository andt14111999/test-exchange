# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::ReprocessService, type: :service do
  let(:service) { described_class.new }
  let(:handler) { instance_double(KafkaService::Handlers::CoinWithdrawalHandler) }
  let(:payload) { { 'eventId' => 'test-123', 'data' => 'test_data' } }
  let(:kafka_event) do
    create(
      :kafka_event,
      event_id: 'test-event-123',
      topic_name: KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE,
      payload: payload,
      status: 'failed'
    )
  end

  before do
    allow(Rails.logger).to receive(:info)
    allow(Rails.logger).to receive(:error)
  end

  describe '#reprocess' do
    context 'when handler exists for topic' do
      before do
        allow(KafkaService::Handlers::CoinWithdrawalHandler).to receive(:new).and_return(handler)
        allow(handler).to receive(:handle)
      end

      it 'successfully reprocesses the event' do
        result = service.reprocess(kafka_event)

        expect(result).to be true
        expect(handler).to have_received(:handle).with(payload)
        expect(kafka_event.reload.status).to eq('processed')
        expect(kafka_event.processed_at).to be_present
      end

      it 'logs the reprocess start' do
        service.reprocess(kafka_event)

        expect(Rails.logger).to have_received(:info).with(
          "Starting reprocess for event: #{kafka_event.event_id}, topic: #{kafka_event.topic_name}"
        )
      end

      it 'logs successful reprocessing' do
        service.reprocess(kafka_event)

        expect(Rails.logger).to have_received(:info).with(
          "Reprocessing event: #{kafka_event.event_id} with handler: #{handler.class.name}"
        )
        expect(Rails.logger).to have_received(:info).with(
          "Event reprocessed successfully: #{kafka_event.event_id}"
        )
      end
    end

    context 'when handler does not exist for topic' do
      let(:kafka_event) do
        create(
          :kafka_event,
          event_id: 'test-event-123',
          topic_name: 'unknown_topic',
          payload: payload,
          status: 'failed'
        )
      end

      it 'returns false and logs error' do
        result = service.reprocess(kafka_event)

        expect(result).to be false
        expect(Rails.logger).to have_received(:error).with(
          'No handler found for topic: unknown_topic'
        )
      end
    end

    context 'when handler processing fails' do
      before do
        allow(KafkaService::Handlers::CoinWithdrawalHandler).to receive(:new).and_return(handler)
        allow(handler).to receive(:handle).and_raise(StandardError.new('Processing failed'))
      end

      it 'returns false' do
        result = service.reprocess(kafka_event)

        expect(result).to be false
      end

      it 'logs the error' do
        service.reprocess(kafka_event)

        expect(Rails.logger).to have_received(:error).with(
          "Failed to reprocess event #{kafka_event.event_id}: Processing failed"
        )
      end
    end

    context 'when database update fails' do
      let(:kafka_event) do
        create(
          :kafka_event,
          event_id: 'test-event-123',
          topic_name: KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE,
          payload: payload,
          status: 'pending'
        )
      end

      before do
        allow(KafkaService::Handlers::CoinWithdrawalHandler).to receive(:new).and_return(handler)
        allow(handler).to receive(:handle)
        allow(kafka_event).to receive(:update!).and_call_original
        allow(kafka_event).to receive(:update!).with(
          processed_at: anything,
          status: 'processed'
        ).and_raise(ActiveRecord::RecordInvalid.new(kafka_event))
      end

      it 'returns false and handles the error' do
        result = service.reprocess(kafka_event)

        expect(result).to be false
        expect(Rails.logger).to have_received(:error).at_least(:once)
      end
    end
  end

      describe 'handler mapping' do
    expected_mappings = {
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
      KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE => KafkaService::Handlers::CoinWithdrawalHandler,
      KafkaService::Config::Topics::TRANSACTION_RESPONSE => KafkaService::Handlers::TransactionResponseHandler
    }

    expected_mappings.each do |topic, handler_class|
      it "maps #{topic} to #{handler_class.name}" do
        kafka_event = create(:kafka_event, topic_name: topic, status: 'failed')
        handler_instance = instance_double(handler_class)

        allow(handler_class).to receive(:new).and_return(handler_instance)
        allow(handler_instance).to receive(:handle)

        service.reprocess(kafka_event)

        expect(handler_class).to have_received(:new)
        expect(handler_instance).to have_received(:handle).with(kafka_event.payload)
      end
    end
  end
end
