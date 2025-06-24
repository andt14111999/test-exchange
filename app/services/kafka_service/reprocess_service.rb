# frozen_string_literal: true

module KafkaService
  class ReprocessService
    def initialize
      @handlers = build_handlers_map
    end

    def reprocess(kafka_event)
      Rails.logger.info("Starting reprocess for event: #{kafka_event.event_id}, topic: #{kafka_event.topic_name}")

      handler = find_handler(kafka_event.topic_name)
      return false unless handler

      process_event_with_handler(kafka_event, handler)
    rescue StandardError => e
      handle_reprocess_error(kafka_event, e)
      false
    end

    private

    def build_handlers_map
      {
        KafkaService::Config::Topics::BALANCE_UPDATE => KafkaService::Handlers::CoinAccountHandler.new,
        KafkaService::Config::Topics::TRANSACTION_RESULT => KafkaService::Handlers::CoinDepositHandler.new,
        KafkaService::Config::Topics::AMM_POOL_UPDATE_TOPIC => KafkaService::Handlers::AmmPoolHandler.new,
        KafkaService::Config::Topics::MERCHANT_ESCROW_UPDATE => KafkaService::Handlers::MerchantEscrowHandler.new,
        KafkaService::Config::Topics::AMM_POSITION_UPDATE_TOPIC => KafkaService::Handlers::AmmPositionHandler.new,
        KafkaService::Config::Topics::OFFER_UPDATE => KafkaService::Handlers::OfferHandler.new,
        KafkaService::Config::Topics::TRADE_UPDATE => KafkaService::Handlers::TradeHandler.new,
        KafkaService::Config::Topics::AMM_ORDER_UPDATE_TOPIC => KafkaService::Handlers::AmmOrderHandler.new,
        KafkaService::Config::Topics::TICK_UPDATE_TOPIC => KafkaService::Handlers::TickHandler.new,
        KafkaService::Config::Topics::BALANCES_LOCK_UPDATE => KafkaService::Handlers::BalanceLockHandler.new,
        KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE => KafkaService::Handlers::CoinWithdrawalHandler.new,
        KafkaService::Config::Topics::TRANSACTION_RESPONSE => KafkaService::Handlers::TransactionResponseHandler.new
      }
    end

    def find_handler(topic_name)
      handler = @handlers[topic_name]
      unless handler
        Rails.logger.error("No handler found for topic: #{topic_name}")
        return nil
      end
      handler
    end

    def process_event_with_handler(kafka_event, handler)
      ActiveRecord::Base.connection_pool.with_connection do
        Rails.logger.info("Reprocessing event: #{kafka_event.event_id} with handler: #{handler.class.name}")

        handler.handle(kafka_event.payload)

        kafka_event.update!(
          processed_at: Time.current,
          status: 'processed'
        )

        Rails.logger.info("Event reprocessed successfully: #{kafka_event.event_id}")
        true
      end
    end

    def handle_reprocess_error(kafka_event, error)
      Rails.logger.error("Failed to reprocess event #{kafka_event.event_id}: #{error.message}")
      Rails.logger.error(error.backtrace.join("\n"))

      kafka_event.update!(status: 'failed')
    end
  end
end
