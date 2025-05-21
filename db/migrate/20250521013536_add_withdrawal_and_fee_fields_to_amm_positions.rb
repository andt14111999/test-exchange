# frozen_string_literal: true

class AddWithdrawalAndFeeFieldsToAmmPositions < ActiveRecord::Migration[8.0]
  def change
    add_column :amm_positions, :amount0_withdrawal, :decimal, precision: 36, scale: 18, default: 0, null: false
    add_column :amm_positions, :amount1_withdrawal, :decimal, precision: 36, scale: 18, default: 0, null: false
    add_column :amm_positions, :estimate_fee_token0, :decimal, precision: 36, scale: 18, default: 0, null: false
    add_column :amm_positions, :estimate_fee_token1, :decimal, precision: 36, scale: 18, default: 0, null: false
    add_column :amm_positions, :apr, :decimal, precision: 10, scale: 6, default: 0, null: false
  end
end
