# frozen_string_literal: true

class AddFieldsToFiatWithdrawals < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    # Add bank transaction fields
    add_column :fiat_withdrawals, :bank_reference, :string
    add_column :fiat_withdrawals, :bank_transaction_date, :datetime
    add_column :fiat_withdrawals, :bank_response_data, :jsonb, default: {}

    # Add verification fields
    add_column :fiat_withdrawals, :verification_status, :string
    add_column :fiat_withdrawals, :verification_attempts, :integer, default: 0

    # Add indexes for common queries
    add_index :fiat_withdrawals, :status, algorithm: :concurrently
    add_index :fiat_withdrawals, [ :status, :created_at ], algorithm: :concurrently
    add_index :fiat_withdrawals, [ :user_id, :created_at ], algorithm: :concurrently
    add_index :fiat_withdrawals, :bank_reference, algorithm: :concurrently
  end
end
