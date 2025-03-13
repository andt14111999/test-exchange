# frozen_string_literal: true

class CreateCoinTransactions < ActiveRecord::Migration[7.0]
  def change
    create_table :coin_transactions do |t|
      t.decimal :amount, precision: 24, scale: 8
      t.references :coin_account, foreign_key: true
      t.string :coin_currency, limit: 14
      t.references :operation, polymorphic: true
      t.decimal :snapshot_balance, precision: 24, scale: 8
      t.decimal :snapshot_frozen_balance, precision: 24, scale: 8
      t.timestamps

      t.index :coin_currency
      t.index :created_at
      t.index %i[operation_type operation_id]
    end
  end
end
