#!/usr/bin/env ruby

# Master bootstrap script to set up NGN merchant user with complete trading setup
require_relative '../../../config/environment'

puts "Starting NGN bootstrap process..."
puts "=" * 50

# List of scripts to run in order
scripts = [
  'create_merchant_user.rb',
  'first_usdt_deposit.rb',
  'first_merchant_escrow.rb',
  'setup_amm_pool.rb',
  'create_amm_position.rb',
  'second_usdt_deposit.rb',
  'second_merchant_escrow.rb',
  'create_offers.rb'
]

scripts.each_with_index do |script, index|
  puts "\n[#{index + 1}/#{scripts.length}] Running #{script}..."
  puts "-" * 30
  
  begin
    load File.join(__dir__, script)
    puts "✓ #{script} completed successfully"
  rescue => e
    puts "✗ Error in #{script}: #{e.message}"
    puts e.backtrace.first(5)
    exit 1
  end
end

puts "\n" + "=" * 50
puts "NGN bootstrap process completed successfully!"
puts "NGN merchant user 'mikeng@example.com' is now ready for trading." 