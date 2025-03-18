# frozen_string_literal: true

class CreateFiatTransactions < ActiveRecord::Migration[8.0]
  def change
    create_table :fiat_transactions do |t|
      t.decimal :amount, precision: 32, scale: 16, null: false
      t.references :fiat_account, null: false, foreign_key: true
      t.string :currency, null: false, limit: 14
      t.references :operation, polymorphic: true
      t.decimal :snapshot_balance, precision: 32, scale: 16
      t.decimal :snapshot_frozen_balance, precision: 32, scale: 16
      t.string :transaction_type

      t.timestamps

      t.index :currency
      t.index :created_at
      t.index %i[operation_type operation_id]
    end
  end
end
