# frozen_string_literal: true

class CreateCoinTransactions < ActiveRecord::Migration[8.0]
  def change
    create_table :coin_transactions do |t|
      t.references :user, null: false, foreign_key: true, index: true
      t.string :coin_type, null: false
      t.decimal :amount, precision: 32, scale: 16, null: false
      t.decimal :fee, precision: 32, scale: 16, default: 0
      t.string :status, null: false, default: 'pending'
      t.string :reference_type
      t.bigint :reference_id

      t.timestamps

      t.index %i[reference_type reference_id]
      t.index :status
      t.index :coin_type
      t.index :created_at
    end
  end
end
