# frozen_string_literal: true

class CreateAmmPools < ActiveRecord::Migration[8.0]
  def change
    create_table :amm_pools do |t|
      t.string :pair, null: false
      t.string :token0, null: false
      t.string :token1, null: false
      t.integer :tick_spacing, null: false
      t.decimal :fee_percentage, precision: 10, scale: 6, null: false
      t.decimal :fee_protocol_percentage, precision: 10, scale: 6, default: 0.0

      # Pool state
      t.integer :current_tick, default: 0
      t.decimal :sqrt_price, precision: 36, scale: 18, default: 1.0
      t.decimal :price, precision: 36, scale: 18, default: 1.0
      t.decimal :liquidity, precision: 36, scale: 18, default: 0.0

      # Fee accumulators
      t.decimal :fee_growth_global0, precision: 36, scale: 18, default: 0.0
      t.decimal :fee_growth_global1, precision: 36, scale: 18, default: 0.0

      # Protocol fees
      t.decimal :protocol_fees0, precision: 36, scale: 18, default: 0.0
      t.decimal :protocol_fees1, precision: 36, scale: 18, default: 0.0

      # Trading statistics
      t.decimal :volume_token0, precision: 36, scale: 18, default: 0.0
      t.decimal :volume_token1, precision: 36, scale: 18, default: 0.0
      t.decimal :volume_usd, precision: 36, scale: 18, default: 0.0
      t.integer :tx_count, default: 0

      # Token reserves
      t.decimal :total_value_locked_token0, precision: 36, scale: 18, default: 0.0
      t.decimal :total_value_locked_token1, precision: 36, scale: 18, default: 0.0

      # Status
      t.string :status, default: 'pending'
      t.string :status_explanation, default: ''

      t.timestamps
    end

    add_index :amm_pools, :pair, unique: true
    add_index :amm_pools, [ :token0, :token1, :fee_percentage ], unique: true
    add_index :amm_pools, :status
  end
end
