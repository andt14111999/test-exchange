#!/usr/bin/env ruby
# frozen_string_literal: true

require File.expand_path(File.join(File.dirname(__FILE__), '..', '..', 'config', 'environment'))

def shutdown
  Rails.logger.info('Shutting down Kafka consumer daemon...')
  @manager&.stop
  Rails.application.exit
end

Signal.trap('TERM') { shutdown }
Signal.trap('INT') { shutdown }

@manager = KafkaService::ConsumerManager.new
@manager.start

Rails.logger.info('Kafka consumer daemon started')

loop { sleep }
