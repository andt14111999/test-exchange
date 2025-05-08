# frozen_string_literal: true

class CreateAmmOrders < ActiveRecord::Migration[6.1]
  def change
    create_table :amm_orders do |t|
      t.references :user, null: false, foreign_key: true
      t.references :amm_pool, null: false, foreign_key: true

      t.string :identifier, null: false, index: { unique: true }
      t.boolean :zero_for_one, null: false, default: true
      t.decimal :amount_specified, null: false, precision: 36, scale: 18, default: 0
      t.decimal :amount_estimated, null: false, precision: 36, scale: 18, default: 0
      t.decimal :amount_actual, null: false, precision: 36, scale: 18, default: 0
      t.integer :before_tick_index
      t.integer :after_tick_index
      t.jsonb :fees, null: false, default: {}
      t.string :status, null: false, default: 'pending'
      t.string :error_message, default: ''
      t.float :slippage, null: false, default: 0.01

      t.timestamps
    end
  end
end
