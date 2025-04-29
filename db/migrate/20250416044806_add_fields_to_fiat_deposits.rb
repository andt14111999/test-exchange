# frozen_string_literal: true

class AddFieldsToFiatDeposits < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    # Add verification-related fields
    add_column :fiat_deposits, :verification_attempts, :integer, default: 0
    add_column :fiat_deposits, :verification_last_attempt_at, :datetime
    add_column :fiat_deposits, :bank_response_data, :jsonb, default: {}

    # Add indexes for common status queries
    add_index :fiat_deposits, :status, algorithm: :concurrently
    add_index :fiat_deposits, [ :status, :created_at ], algorithm: :concurrently
    add_index :fiat_deposits, [ :user_id, :created_at ], algorithm: :concurrently
    add_index :fiat_deposits, :money_sent_at, algorithm: :concurrently
  end
end
