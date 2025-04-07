# frozen_string_literal: true

class AddInitPriceToAmmPools < ActiveRecord::Migration[8.0]
  def change
    add_column :amm_pools, :init_price, :decimal, precision: 36, scale: 18, default: nil
  end
end
