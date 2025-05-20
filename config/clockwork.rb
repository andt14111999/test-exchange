# frozen_string_literal: true

require 'clockwork'
require './config/boot'
require './config/environment'

module Clockwork
  # every(1.day, 'runing_test', at: '00:00') do
  #   TestingJob.perform_async
  # end

  every(1.minute, 'trade_timeout_checks') do
    TradeTimeoutChecksJob.perform_async
  end
end
