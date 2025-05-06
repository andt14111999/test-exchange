# frozen_string_literal: true

class CreateFiatDeposits < ActiveRecord::Migration[8.0]
  def change
    create_table :fiat_deposits do |t|
      t.references :user, null: false, foreign_key: true
      t.references :fiat_account, null: false, foreign_key: true
      t.string :currency, null: false
      t.string :country_code, null: false
      t.decimal :fiat_amount, precision: 32, scale: 16, null: false
      t.decimal :original_fiat_amount, precision: 32, scale: 16
      t.decimal :deposit_fee, precision: 32, scale: 16, default: 0

      # Transaction info
      t.string :explorer_ref
      t.string :memo, null: false
      t.jsonb :fiat_deposit_details, default: {}

      # Ownership verification
      t.string :ownership_proof_url
      t.string :sender_name
      t.string :sender_account_number

      # Status
      t.string :status, default: 'awaiting', null: false
      t.string :cancel_reason

      # Timestamps
      t.datetime :processed_at
      t.datetime :cancelled_at
      t.datetime :money_sent_at

      # Link to Trade
      t.string :payable_type
      t.bigint :payable_id

      t.timestamps

      t.index [ :user_id, :status ]
      t.index :explorer_ref
      t.index :memo, unique: true
      t.index [ :payable_type, :payable_id ]
      t.index :country_code
    end
  end
end
