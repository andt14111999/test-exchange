# frozen_string_literal: true

module KafkaService
  module Services
    module AmmPosition
      class AmmPositionService < KafkaService::Base::Service
        def create(identifier:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::AMM_POSITION_TOPIC,
            key: identifier,
            data: payload
          )
        end

        def collect_fee(identifier:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::AMM_POSITION_TOPIC,
            key: identifier,
            data: payload
          )
        end

        def close(identifier:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::AMM_POSITION_TOPIC,
            key: identifier,
            data: payload
          )
        end
      end
    end
  end
end
