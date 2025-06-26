# frozen_string_literal: true

class SendEventCompleteWithdrawalToKafkaForCompletedWithdrawals < ActiveRecord::Migration[7.0]
  def up
    # Find completed coin withdrawals with coin_address present
    completed_withdrawals = CoinWithdrawal.where(status: 'completed')
                                          .where.not(coin_address: [ nil, '' ])

    puts "Found #{completed_withdrawals.count} completed withdrawals with coin_address to process"

    completed_withdrawals.find_each do |withdrawal|
      puts "Processing withdrawal ID: #{withdrawal.id}"
      withdrawal.send(:send_event_complete_withdrawal_to_kafka)
    rescue StandardError => e
      puts "Error processing withdrawal ID #{withdrawal.id}: #{e.message}"
      # Continue processing other withdrawals even if one fails
    end

    puts "Completed processing withdrawals"
  end

  def down
    # This migration cannot be reversed as it only sends events to Kafka
    puts "This migration cannot be reversed"
  end
end
