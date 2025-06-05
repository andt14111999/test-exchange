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
        KafkaService::Config::Topics::TRADE_UPDATE => KafkaService::Handlers::TradeHandler.new,
        KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC => KafkaService::Handlers::AmmOrderHandler.new,
        KafkaService::Config::Topics::TICK_UPDATE_TOPIC => KafkaService::Handlers::TickHandler.new,
        KafkaService::Config::Topics::BALANCES_LOCK_UPDATE => KafkaService::Handlers::BalanceLockHandler.new
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
            process_message_with_retry(topic, handler, payload)
          end
        rescue StandardError => e
          Rails.logger.error("Failed to start consumer for topic #{topic}: #{e.message}")
          restart_consumer(topic, handler)
        end
      end

      @monitor.synchronize { @consumers << consumer }
    end

    def process_message_with_retry(topic, handler, payload)
      Retriable.retriable(
        tries: 3,
        base_interval: 1,
        multiplier: 2,
        on: [ StandardError ]
      ) do
        ActiveRecord::Base.connection_pool.with_connection do
          process_message(topic, handler, payload)
        end
      end
    rescue StandardError => e
      Rails.logger.error("Failed to process message after retries: #{e.message}")
    end

    def process_message(topic, handler, payload)
      Rails.logger.info("Received message for topic #{topic}: #{payload}")

      # Parse payload if it's a string
      payload = JSON.parse(payload) if payload.is_a?(String)

      # Get event ID from payload
      event_id = payload['messageId'] || payload['eventId']
      return if event_id.blank?

      Rails.logger.info("Processing event: #{event_id} for topic: #{topic}")

      # Check if event already exists
      if KafkaEvent.where(event_id: event_id, topic_name: topic).exists?
        Rails.logger.info("Duplicate event detected, skipping processing: #{event_id}")
        return
      end

      # Store event first
      kafka_event = store_event(topic, event_id, payload)
      return unless kafka_event

      # Process the event
      handler.handle(payload)

      # Mark event as processed
      kafka_event.update!(processed_at: Time.current)
      Rails.logger.info("Event processed successfully: #{event_id}")
    rescue JSON::ParserError => e
      Rails.logger.error("Failed to parse message: #{e.message}")
      Rails.logger.error("Raw message: #{payload}")
    rescue StandardError => e
      Rails.logger.error("Error processing message: #{e.message}")
      Rails.logger.error(e.backtrace.join("\n"))
    end

    def store_event(topic, event_id, payload)
      Rails.logger.info("Storing event: #{event_id}")
      KafkaEvent.create!(
        event_id: event_id,
        topic_name: topic,
        payload: payload,
        status: 'received',
      )
    rescue StandardError => e
      Rails.logger.error("Failed to store event: #{e.message}")
      Rails.logger.error(e.backtrace.join("\n"))
      nil
    end

    def restart_consumer(topic, handler)
      sleep 5
      start_consumer_with_monitor(topic, handler)
    end
  end
end
