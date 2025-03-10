# frozen_string_literal: true

class CreateCoinDeposits < ActiveRecord::Migration[8.0]
  def change
    create_table :coin_deposits do |t|
      t.references :user, null: false, foreign_key: true, index: true
      t.references :coin_account, null: false, foreign_key: true, index: true
      t.string :coin_type, null: false
      t.decimal :amount, precision: 32, scale: 16, null: false
      t.decimal :fee, precision: 32, scale: 16, default: 0
      t.string :tx_hash
      t.string :reference_id
      t.integer :confirmations, default: 0
      t.string :status, default: 'pending'
      t.decimal :blockchain_fee, precision: 32, scale: 16

      t.timestamps

      t.index :tx_hash, unique: true
      t.index :reference_id
      t.index :status
      t.index :created_at
    end
  end
end
