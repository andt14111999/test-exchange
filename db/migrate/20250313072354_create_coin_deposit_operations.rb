# frozen_string_literal: true

class CreateCoinDepositOperations < ActiveRecord::Migration[7.0]
  def change
    create_table :coin_deposit_operations do |t|
      t.references :coin_account, foreign_key: true
      t.decimal :coin_amount, precision: 24, scale: 8
      t.string :coin_currency, limit: 5
      t.references :coin_deposit, foreign_key: true
      t.decimal :coin_fee, precision: 24, scale: 8, default: 0.0
      t.decimal :platform_fee, precision: 24, scale: 8, default: 0.0
      t.string :tx_hash
      t.integer :out_index
      t.string :status
      t.text :status_explanation
      t.timestamps

      t.index :tx_hash
      t.index :status
      t.index %i[tx_hash out_index], unique: true
    end
  end
end
