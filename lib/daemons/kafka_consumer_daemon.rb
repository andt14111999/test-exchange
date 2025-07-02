#!/usr/bin/env ruby
# frozen_string_literal: true

require File.expand_path(File.join(File.dirname(__FILE__), '..', '..', 'config', 'environment'))

module Daemons
  class KafkaConsumerDaemon
    def self.run
      new.run
    end

    def initialize
      @shutdown_requested = false
    end

    def run
      setup_signal_handlers
      start_manager

      Rails.logger.info('Kafka consumer daemon started')

      # Main loop with periodic shutdown checks
      until @shutdown_requested
        sleep 1
      end

      perform_shutdown
    end

    private

    def setup_signal_handlers
      # Use simple flag setting in signal handlers to avoid trap context issues
      Signal.trap('TERM') { @shutdown_requested = true }
      Signal.trap('INT') { @shutdown_requested = true }
    end

    def start_manager
      @manager = KafkaService::ConsumerManager.new
      @manager.start
    end

    def perform_shutdown
      Rails.logger.info('Shutting down Kafka consumer daemon...')
      @manager&.stop
      Rails.logger.info('Kafka consumer daemon shutdown complete')
      Kernel.exit
    end
  end
end

# Only run the daemon if this file is executed directly (not required/loaded)
Daemons::KafkaConsumerDaemon.run if __FILE__ == $PROGRAM_NAME
