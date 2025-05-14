class AddCoinFeeToInternalTransfers < ActiveRecord::Migration[8.0]
  def change
    add_column :coin_internal_transfer_operations, :coin_fee, :decimal, precision: 36, scale: 18, default: 0
  end
end
