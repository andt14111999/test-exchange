# frozen_string_literal: true

class CreateCoinWithdrawals < ActiveRecord::Migration[8.0]
  def change
    create_table :coin_withdrawals do |t|
      t.references :user, null: false, foreign_key: true, index: true
      t.references :coin_account, null: false, foreign_key: true, index: true
      t.string :coin_type, null: false
      t.decimal :amount, precision: 32, scale: 16, null: false
      t.decimal :fee, precision: 32, scale: 16, default: 0
      t.decimal :blockchain_fee, precision: 32, scale: 16
      t.string :destination_address, null: false
      t.string :memo
      t.string :network
      t.string :tx_hash
      t.string :reference_id
      t.string :status, default: 'pending'

      t.timestamps

      t.index :tx_hash
      t.index :reference_id
      t.index :status
      t.index :created_at
    end
  end
end
