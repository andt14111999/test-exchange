# frozen_string_literal: true

class CreateAmmPositions < ActiveRecord::Migration[7.0]
  def change
    create_table :amm_positions do |t|
      t.references :user, null: false, foreign_key: true, index: true
      t.references :amm_pool, null: false, foreign_key: true, index: true
      t.string :identifier, null: false, index: { unique: true }
      t.string :status, null: false, default: 'pending', index: true
      t.decimal :liquidity, null: false, default: 0
      t.decimal :slippage, null: false, default: 1.0
      t.integer :tick_lower_index, null: false
      t.integer :tick_upper_index, null: false
      t.decimal :amount0, null: false, default: 0
      t.decimal :amount1, null: false, default: 0
      t.decimal :amount0_initial, null: false, default: 0
      t.decimal :amount1_initial, null: false, default: 0
      t.decimal :fee_growth_inside0_last, null: false, default: 0
      t.decimal :fee_growth_inside1_last, null: false, default: 0
      t.decimal :tokens_owed0, null: false, default: 0
      t.decimal :tokens_owed1, null: false, default: 0
      t.decimal :fee_collected0, null: false, default: 0
      t.decimal :fee_collected1, null: false, default: 0
      t.string :error_message

      t.timestamps
    end
  end
end
