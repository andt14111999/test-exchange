# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::ConsumerManager, type: :service do
  let(:coin_account_handler) { instance_double(KafkaService::Handlers::CoinAccountHandler) }
  let(:coin_deposit_handler) { instance_double(KafkaService::Handlers::CoinDepositHandler) }
  let(:amm_pool_handler) { instance_double(KafkaService::Handlers::AmmPoolHandler) }
  let(:consumer) { instance_double(KafkaService::Base::Consumer) }
  let(:payload) { { 'data' => 'test' } }

  before do
    allow(KafkaService::Handlers::CoinAccountHandler).to receive(:new).and_return(coin_account_handler)
    allow(KafkaService::Handlers::CoinDepositHandler).to receive(:new).and_return(coin_deposit_handler)
    allow(KafkaService::Handlers::AmmPoolHandler).to receive(:new).and_return(amm_pool_handler)
    allow(KafkaService::Base::Consumer).to receive(:new).and_return(consumer)
    allow(consumer).to receive(:start)
    allow(consumer).to receive(:stop)
  end

  describe '#initialize' do
    it 'initializes with correct handlers' do
      manager = described_class.new

      expect(manager.instance_variable_get(:@handlers)).to eq({
        KafkaService::Config::Topics::BALANCE_UPDATE => coin_account_handler,
        KafkaService::Config::Topics::TRANSACTION_RESULT => coin_deposit_handler,
        KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC => amm_pool_handler
      })
    end
  end

  describe '#start' do
    it 'starts consumers for each topic' do
      manager = described_class.new

      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::BALANCE_UPDATE}_processor",
        topics: [ KafkaService::Config::Topics::BALANCE_UPDATE ]
      )
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::TRANSACTION_RESULT}_processor",
        topics: [ KafkaService::Config::Topics::TRANSACTION_RESULT ]
      )
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC}_processor",
        topics: [ KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC ]
      )

      manager.start
    end
  end

  describe '#stop' do
    it 'stops all consumers' do
      manager = described_class.new
      manager.start

      expect(consumer).to receive(:stop).exactly(3).times
      manager.stop
    end
  end

  describe 'message processing' do
    it 'processes messages with retries' do
      manager = described_class.new
      allow(Retriable).to receive(:retriable).and_yield

      expect(coin_account_handler).to receive(:handle).with(payload)
      manager.send(:process_message_with_retry, coin_account_handler, payload)
    end

    it 'logs error when message processing fails after retries' do
      manager = described_class.new
      allow(Retriable).to receive(:retriable).and_raise(StandardError.new('test error'))

      expect(Rails.logger).to receive(:error).with('Failed to process message after retries: test error')
      manager.send(:process_message_with_retry, coin_account_handler, payload)
    end
  end

  describe 'consumer restart' do
    it 'restarts consumer after error' do
      manager = described_class.new
      allow(Thread).to receive(:new).and_yield
      allow(Rails.application.reloader).to receive(:wrap).and_yield
      allow(consumer).to receive(:start).and_raise(StandardError.new('test error'))
      allow(manager).to receive(:restart_consumer)

      # Only test the first consumer (BALANCE_UPDATE)
      expect(Rails.logger).to receive(:error).with('Failed to start consumer for topic EE.O.coin_account_update: test error')
      expect(manager).to receive(:restart_consumer).with(KafkaService::Config::Topics::BALANCE_UPDATE, coin_account_handler)

      # Stop after first consumer
      expect(manager).to receive(:start_consumer_with_monitor).once.with(KafkaService::Config::Topics::BALANCE_UPDATE, coin_account_handler).and_call_original
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::TRANSACTION_RESULT, coin_deposit_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC, amm_pool_handler)

      manager.start
    end
  end
end
