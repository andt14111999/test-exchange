# frozen_string_literal: true

class CreateCoinWithdrawalOperations < ActiveRecord::Migration[7.0]
  def change
    create_table :coin_withdrawal_operations, id: :serial do |t|
      t.decimal :coin_amount, precision: 24, scale: 8
      t.string :coin_currency, limit: 14
      t.decimal :coin_fee, precision: 24, scale: 8, default: 0.0
      t.references :coin_withdrawal
      t.datetime :scheduled_at
      t.string :status
      t.text :status_explanation
      t.string :tx_hash
      t.jsonb :withdrawal_data
      t.string :withdrawal_status

      t.timestamps

      t.index :status
      t.index :withdrawal_status
      t.index :tx_hash
      t.index :scheduled_at
      t.index :created_at
    end
  end
end
