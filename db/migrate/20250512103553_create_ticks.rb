# frozen_string_literal: true

class CreateTicks < ActiveRecord::Migration[8.0]
  def change
    create_table :ticks do |t|
      t.string :pool_pair, null: false
      t.integer :tick_index, null: false
      t.decimal :liquidity_gross, precision: 36, scale: 18, default: 0
      t.decimal :liquidity_net, precision: 36, scale: 18, default: 0
      t.decimal :fee_growth_outside0, precision: 36, scale: 18, default: 0
      t.decimal :fee_growth_outside1, precision: 36, scale: 18, default: 0
      t.bigint :tick_initialized_timestamp
      t.boolean :initialized, default: false
      t.string :status, default: 'inactive'
      t.string :tick_key, null: false
      t.references :amm_pool, foreign_key: true
      t.bigint :created_at_timestamp
      t.bigint :updated_at_timestamp

      t.timestamps

      t.index :pool_pair
      t.index :tick_index
      t.index :tick_key, unique: true
      t.index [ :pool_pair, :tick_index ], unique: true
      t.index :status
    end
  end
end
