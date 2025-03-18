# frozen_string_literal: true

class CreateMerchantEscrows < ActiveRecord::Migration[8.0]
  def change
    create_table :merchant_escrows do |t|
      t.references :user, null: false, foreign_key: true
      t.references :usdt_account, null: false, foreign_key: { to_table: :coin_accounts }
      t.references :fiat_account, null: false, foreign_key: true
      t.decimal :usdt_amount, precision: 32, scale: 16, null: false
      t.decimal :fiat_amount, precision: 32, scale: 16, null: false
      t.string :fiat_currency, null: false
      t.string :status, null: false
      t.datetime :completed_at

      t.timestamps

      t.index :status
      t.index :fiat_currency
      t.index :created_at
    end
  end
end
