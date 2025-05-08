# frozen_string_literal: true

module KafkaService
  module Services
    module AmmOrder
      class AmmOrderService < KafkaService::Base::Service
        def create(identifier:, payload:)
          send_event(
            topic: KafkaService::Config::Topics::AMM_ORDER_TOPIC,
            key: identifier,
            data: payload
          )
        end
      end
    end
  end
end
