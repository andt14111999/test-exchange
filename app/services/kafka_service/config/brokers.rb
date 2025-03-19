# frozen_string_literal: true

module KafkaService
  module Config
    module Brokers
      BROKERS = [ ENV.fetch('KAFKA_BROKER') || '127.0.0.1:9092' ].freeze
    end

    module Config
      CONSUMER_CONFIG = {
        offset_commit_interval: 5,
        offset_commit_threshold: 100,
        offset_retention_time: 7_200,
        fetcher_max_queue_size: 100,
        session_timeout: 30,
        heartbeat_interval: 10
      }.freeze

      PRODUCER_CONFIG = {
        required_acks: :all,
        compression_codec: :snappy,
        max_retries: 3,
        retry_backoff: 1,
        max_buffer_size: 10_000,
        max_buffer_bytesize: 10_485_760
      }.freeze
    end
  end
end
