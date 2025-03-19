#!/usr/bin/env ruby
# frozen_string_literal: true

require File.expand_path(File.join(File.dirname(__FILE__), '..', '..', 'config', 'environment'))

def shutdown
  @manager&.stop
  Process.exit(0)
end

Signal.trap('TERM') { shutdown }
Signal.trap('INT') { shutdown }

@manager = KafkaService::ConsumerManager.new
@manager.start

# Keep the script running
loop { sleep }
