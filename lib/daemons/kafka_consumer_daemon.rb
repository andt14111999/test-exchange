#!/usr/bin/env ruby
# frozen_string_literal: true

require File.expand_path(File.join(File.dirname(__FILE__), '..', '..', 'config', 'environment'))

module Daemons
  class KafkaConsumerDaemon
    def self.run
      new.run
    end

    def run
      setup_signal_handlers
      start_manager

      Rails.logger.info('Kafka consumer daemon started')

      loop { sleep }
    end

    private

    def setup_signal_handlers
      Signal.trap('TERM') { shutdown }
      Signal.trap('INT') { shutdown }
    end

    def start_manager
      @manager = KafkaService::ConsumerManager.new
      @manager.start
    end

    def shutdown
      Rails.logger.info('Shutting down Kafka consumer daemon...')
      @manager&.stop
      Rails.application.exit
    end
  end
end

# Only run the daemon if this file is executed directly (not required/loaded)
Daemons::KafkaConsumerDaemon.run if __FILE__ == $PROGRAM_NAME
