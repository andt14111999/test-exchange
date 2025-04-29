# frozen_string_literal: true

require 'clockwork'
require_relative '../../config/boot'
require_relative '../../config/environment'

module Schedule
  include Clockwork

  handler do |job|
    puts "Running #{job}"
  end

  # Run the trade expiry job every 5 minutes
  every(5.minutes, 'trade_expiry_job') do
    TradeExpiryJob.perform_later
  end

  # Run the fiat deposit timeout check every 1 hour
  every(1.hour, 'fiat_deposit_timeout_check') do
    FiatDeposit.timeout_candidates.find_each(&:timeout_check!)
  end

  # Run the fiat deposit verification timeout check every 2 hours
  every(2.hours, 'fiat_deposit_verification_timeout') do
    FiatDeposit.ready.where('updated_at < ?', 2.hours.ago).find_each(&:verification_timeout_check!)
  end
end
