# frozen_string_literal: true

class CreateFiatWithdrawals < ActiveRecord::Migration[8.0]
  def change
    create_table :fiat_withdrawals do |t|
      t.references :user, null: false, foreign_key: true
      t.references :fiat_account, null: false, foreign_key: true
      t.string :currency, null: false
      t.string :country_code, null: false
      t.decimal :fiat_amount, precision: 32, scale: 16, null: false
      t.decimal :fee, precision: 32, scale: 16, default: 0
      t.decimal :amount_after_transfer_fee, precision: 32, scale: 16

      # Bank info
      t.string :bank_name, null: false
      t.string :bank_account_name, null: false
      t.string :bank_account_number, null: false
      t.string :bank_branch

      # Status
      t.string :status, default: 'pending', null: false

      # Error handling
      t.integer :retry_count, default: 0
      t.text :error_message
      t.string :cancel_reason

      # Timestamps
      t.datetime :processed_at
      t.datetime :cancelled_at

      # Link to Trade
      t.string :withdrawable_type
      t.bigint :withdrawable_id

      t.timestamps

      t.index [ :user_id, :status ]
      t.index [ :withdrawable_type, :withdrawable_id ]
      t.index :country_code
    end
  end
end
