# frozen_string_literal: true

module KafkaService
  class ConsumerManager
    def initialize
      @handlers = {
        KafkaService::Config::Topics::BALANCE_UPDATE => KafkaService::Handlers::CoinAccountHandler.new,
        KafkaService::Config::Topics::TRANSACTION_RESULT => KafkaService::Handlers::CoinDepositHandler.new,
        KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC => KafkaService::Handlers::AmmPoolHandler.new,
        KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE => KafkaService::Handlers::MerchantEscrowHandler.new,
        KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC => KafkaService::Handlers::AmmPositionHandler.new,
        KafkaService::Config::Topics::OFFER_UPDATE => KafkaService::Handlers::OfferHandler.new,
        KafkaService::Config::Topics::TRADE_UPDATE => KafkaService::Handlers::TradeHandler.new
      }
      @consumers = []
      @monitor = Monitor.new
    end

    def start
      @handlers.each do |topic, handler|
        start_consumer_with_monitor(topic, handler)
      end
    end

    def stop
      @monitor.synchronize do
        @consumers.each(&:stop)
        @consumers.clear
      end
    end

    private

    def start_consumer_with_monitor(topic, handler)
      consumer = KafkaService::Base::Consumer.new(
        group_id: "#{Rails.env}_#{topic}_processor",
        topics: [ topic ]
      )

      Thread.new do
        Rails.application.reloader.wrap do
          consumer.start do |_topic, payload|
            process_message_with_retry(handler, payload)
          end
        rescue StandardError => e
          Rails.logger.error("Failed to start consumer for topic #{topic}: #{e.message}")
          restart_consumer(topic, handler)
        end
      end

      @monitor.synchronize { @consumers << consumer }
    end

    def process_message_with_retry(handler, payload)
      Retriable.retriable(
        tries: 3,
        base_interval: 1,
        multiplier: 2,
        on: [ StandardError ]
      ) do
        ActiveRecord::Base.connection_pool.with_connection do
          handler.handle(payload)
        end
      end
    rescue StandardError => e
      Rails.logger.error("Failed to process message after retries: #{e.message}")
    end

    def restart_consumer(topic, handler)
      sleep 5
      start_consumer_with_monitor(topic, handler)
    end
  end
end
