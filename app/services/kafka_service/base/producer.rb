# frozen_string_literal: true

module KafkaService
  module Base
    class Producer
      def initialize
        @logger = Rails.env.production? ? Rails.logger : Logger.new('log/kafka_producer.log')
        @producer = initialize_producer
        @logger.info("RdKafka Producer initialized with brokers: #{KafkaService::Config::Brokers::BROKERS}")
      end

      def send_message(topic:, key:, payload:)
        @producer.produce(
          topic: topic,
          payload: payload.to_json,
          key: key
        )
        @producer.flush
      rescue StandardError => e
        @logger.error("Failed to send message: #{e.message}")
        raise
      end

      def send_messages_batch(messages, batch_size = 1000)
        process_batch(messages, batch_size)
      end

      def close
        @producer.close
        @logger.info('RdKafka Producer closed')
      end

      private

      def rdkafka_config
        config = {
          'bootstrap.servers': KafkaService::Config::Brokers::BROKERS.join(','),
          'client.id': "base_portal_#{Rails.env}",
          'enable.idempotence': true,
          'retries': 3,
          'retry.backoff.ms': 1000,
          'compression.type': 'snappy',
          'batch.size': 16384,
          'linger.ms': 5
        }

        if ssl_enabled?
          config.merge!(ssl_config)
        end

        config
      end

      def ssl_enabled?
        ENV.fetch('KAFKA_SSL_ENABLED', 'false') == 'true'
      end

      def ssl_config
        {
          'security.protocol': 'SSL',
          'ssl.ca.location': '/etc/ssl/certs/ca-certificates.crt',
          'ssl.verify.hostname': 'false'
        }
      end

      def initialize_producer
        Rdkafka::Config.new(rdkafka_config).producer
      end

      def process_batch(messages, batch_size)
        total_messages = messages.size
        batches = (total_messages.to_f / batch_size).ceil
        @logger.info("Sending #{total_messages} messages in #{batches} batches")

        messages.each_slice(batch_size).with_index do |batch, index|
          send_batch(batch, index, batches, batch_size)
        end
      end

      def send_batch(batch, index, total_batches, batch_size)
        batch.each do |msg|
          @producer.produce(
            topic: msg[:topic],
            payload: msg[:payload].to_json,
            key: msg[:key]
          )
        end

        @producer.flush
        @logger.info("Batch #{index + 1}/#{total_batches} sent successfully")
        sleep(0.1) if index < total_batches - 1
      rescue Rdkafka::RdkafkaError => e
        handle_batch_failure(batch, index, batch_size, e)
      end

      def handle_batch_failure(batch, index, batch_size, error)
        @logger.error("Failed to deliver batch #{index + 1}: #{error.message}")

        if batch_size > 100
          new_batch_size = batch_size / 2
          @logger.info("Retrying with smaller batch size: #{new_batch_size}")
          send_messages_batch(batch, new_batch_size)
        else
          @logger.error('Could not deliver messages even with small batch size')
          raise
        end
      end
    end
  end
end
