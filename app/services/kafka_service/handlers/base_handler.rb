# frozen_string_literal: true

module KafkaService
  module Handlers
    class BaseHandler
      def handle(payload)
        raise NotImplementedError, 'Subclasses must implement handle method'
      end
    end
  end
end
