# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::ConsumerManager, type: :service do
  let(:coin_account_handler) { instance_double(KafkaService::Handlers::CoinAccountHandler) }
  let(:coin_deposit_handler) { instance_double(KafkaService::Handlers::CoinDepositHandler) }
  let(:amm_pool_handler) { instance_double(KafkaService::Handlers::AmmPoolHandler) }
  let(:merchant_escrow_handler) { instance_double(KafkaService::Handlers::MerchantEscrowHandler) }
  let(:amm_position_handler) { instance_double(KafkaService::Handlers::AmmPositionHandler) }
  let(:offer_handler) { instance_double(KafkaService::Handlers::OfferHandler) }
  let(:trade_handler) { instance_double(KafkaService::Handlers::TradeHandler) }
  let(:amm_order_handler) { instance_double(KafkaService::Handlers::AmmOrderHandler) }
  let(:tick_handler) { instance_double(KafkaService::Handlers::TickHandler) }
  let(:balance_lock_handler) { instance_double(KafkaService::Handlers::BalanceLockHandler) }
  let(:consumer) { instance_double(KafkaService::Base::Consumer) }
  let(:payload) { { 'data' => 'test' } }

  before do
    allow(KafkaService::Handlers::CoinAccountHandler).to receive(:new).and_return(coin_account_handler)
    allow(KafkaService::Handlers::CoinDepositHandler).to receive(:new).and_return(coin_deposit_handler)
    allow(KafkaService::Handlers::AmmPoolHandler).to receive(:new).and_return(amm_pool_handler)
    allow(KafkaService::Handlers::MerchantEscrowHandler).to receive(:new).and_return(merchant_escrow_handler)
    allow(KafkaService::Handlers::AmmPositionHandler).to receive(:new).and_return(amm_position_handler)
    allow(KafkaService::Handlers::OfferHandler).to receive(:new).and_return(offer_handler)
    allow(KafkaService::Handlers::TradeHandler).to receive(:new).and_return(trade_handler)
    allow(KafkaService::Handlers::AmmOrderHandler).to receive(:new).and_return(amm_order_handler)
    allow(KafkaService::Handlers::TickHandler).to receive(:new).and_return(tick_handler)
    allow(KafkaService::Handlers::BalanceLockHandler).to receive(:new).and_return(balance_lock_handler)
    allow(KafkaService::Base::Consumer).to receive(:new).and_return(consumer)
    allow(consumer).to receive(:start)
    allow(consumer).to receive(:stop)
  end

  describe '#initialize' do
    it 'initializes with correct handlers' do
      manager = described_class.new

      # Create a new hash with our expected handlers
      expected_handlers = {
        KafkaService::Config::Topics::BALANCE_UPDATE => coin_account_handler,
        KafkaService::Config::Topics::TRANSACTION_RESULT => coin_deposit_handler,
        KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC => amm_pool_handler,
        KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE => merchant_escrow_handler,
        KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC => amm_position_handler,
        KafkaService::Config::Topics::OFFER_UPDATE => offer_handler,
        KafkaService::Config::Topics::TRADE_UPDATE => trade_handler,
        KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC => amm_order_handler,
        KafkaService::Config::Topics::TICK_UPDATE_TOPIC => tick_handler,
        KafkaService::Config::Topics::BALANCES_LOCK_UPDATE => balance_lock_handler
      }

      # Use a custom matcher instead of eq to verify the keys and values independently
      # This is safer than direct comparison if the order changes
      actual_handlers = manager.instance_variable_get(:@handlers)

      expect(actual_handlers.keys).to match_array(expected_handlers.keys)

      # Check each handler individually
      expected_handlers.each do |topic, handler|
        expect(actual_handlers[topic]).to eq(handler)
      end
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
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE}_processor",
        topics: [ KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE ]
      )
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC}_processor",
        topics: [ KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC ]
      )
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::OFFER_UPDATE}_processor",
        topics: [ KafkaService::Config::Topics::OFFER_UPDATE ]
      )
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::TRADE_UPDATE}_processor",
        topics: [ KafkaService::Config::Topics::TRADE_UPDATE ]
      )
      expect(KafkaService::Base::Consumer).to receive(:new).with(
        group_id: "#{Rails.env}_#{KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC}_processor",
        topics: [ KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC ]
      )

      manager.start
    end
  end

  describe '#stop' do
    it 'stops all consumers' do
      manager = described_class.new
      manager.start

      # Adjust the count to match the actual number of handlers
      expect(consumer).to receive(:stop).exactly(10).times
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

      # Stub all other consumer startups to isolate the test
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::TRANSACTION_RESULT, coin_deposit_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC, amm_pool_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE, merchant_escrow_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC, amm_position_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::OFFER_UPDATE, offer_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::TRADE_UPDATE, trade_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC, amm_order_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::TICK_UPDATE_TOPIC, tick_handler)
      allow(manager).to receive(:start_consumer_with_monitor).with(KafkaService::Config::Topics::BALANCES_LOCK_UPDATE, balance_lock_handler)

      expect(Rails.logger).to receive(:error).with('Failed to start consumer for topic EE.O.coin_account_update: test error')
      expect(manager).to receive(:restart_consumer).with(KafkaService::Config::Topics::BALANCE_UPDATE, coin_account_handler)

      # Let it call the original for just this topic
      expect(manager).to receive(:start_consumer_with_monitor).once.with(
        KafkaService::Config::Topics::BALANCE_UPDATE,
        coin_account_handler
      ).and_call_original

      manager.start
    end
  end
end
