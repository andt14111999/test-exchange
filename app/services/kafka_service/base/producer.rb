# frozen_string_literal: true

module KafkaService
  module Base
    class Producer
      def initialize
        @logger = Rails.env.production? ? Rails.logger : Logger.new('log/kafka_producer.log')
        @kafka = ::Kafka.new(
          KafkaService::Config::Brokers::BROKERS,
          **kafka_config
        )
        @producer = initialize_producer
        @logger.info("Kafka Producer initialized with brokers: #{KafkaService::Config::Brokers::BROKERS}")
      end

      def send_message(topic:, key:, payload:)
        @producer.produce(payload.to_json, topic: topic, key: key)
        @producer.deliver_messages
      rescue StandardError => e
        @logger.error("Failed to send message: #{e.message}")
        raise
      end

      def send_messages_batch(messages, batch_size = 1000)
        process_batch(messages, batch_size)
      end

      def close
        @producer.shutdown
        @logger.info('Kafka Producer closed')
      end

      private

      def kafka_config
        config = {
          client_id: "base_portal_#{Rails.env}",
          logger: @logger,
          socket_timeout: 20,
          connect_timeout: 20
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
          ssl_ca_cert_file_path: '/etc/ssl/certs/ca-certificates.crt', # Default CA bundle for AWS MSK
          ssl_client_cert: nil, # Not needed for TLS-only (no client cert auth)
          ssl_client_cert_key: nil, # Not needed for TLS-only (no client cert auth)
          ssl_verify_hostname: false
        }
      end

      def initialize_producer
        @kafka.producer(**KafkaService::Config::Config::PRODUCER_CONFIG)
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
          @producer.produce(msg[:payload].to_json, topic: msg[:topic], key: msg[:key])
        end

        @producer.deliver_messages
        @logger.info("Batch #{index + 1}/#{total_batches} sent successfully")
        sleep(0.1) if index < total_batches - 1
      rescue ::Kafka::DeliveryFailed => e
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
