# frozen_string_literal: true

class CreateMerchantEscrowOperations < ActiveRecord::Migration[8.0]
  def change
    create_table :merchant_escrow_operations do |t|
      t.references :merchant_escrow, null: false, foreign_key: true
      t.references :usdt_account, null: false, foreign_key: { to_table: :coin_accounts }
      t.references :fiat_account, null: false, foreign_key: true
      t.decimal :usdt_amount, precision: 32, scale: 16, null: false
      t.decimal :fiat_amount, precision: 32, scale: 16, null: false
      t.string :fiat_currency, null: false
      t.string :operation_type, null: false
      t.string :status, null: false
      t.text :status_explanation

      t.timestamps

      t.index :status
      t.index :operation_type
      t.index :created_at
    end
  end
end
