# frozen_string_literal: true

module KafkaService
  module Services
    module AmmPool
      class AmmPoolService < KafkaService::Base::Service
        def create(pair:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::AMM_POOL_TOPIC,
            key: pair,
            data: payload
          )
        end

        def update(pair:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::AMM_POOL_TOPIC,
            key: pair,
            data: payload
          )
        end
      end
    end
  end
end
