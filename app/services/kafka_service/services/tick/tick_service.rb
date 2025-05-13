# frozen_string_literal: true

module KafkaService
  module Services
    module Tick
      class TickService < KafkaService::Base::Service
        def query(pool_pair:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::TICK_QUERY_TOPIC,
            key: pool_pair,
            data: payload
          )
        end
      end
    end
  end
end
