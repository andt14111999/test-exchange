class AddFeeFieldsToTrades < ActiveRecord::Migration[8.0]
  def change
    add_column :trades, :fixed_fee, :decimal, precision: 32, scale: 16, default: 0
    add_column :trades, :total_fee, :decimal, precision: 32, scale: 16, default: 0
    add_column :trades, :amount_after_fee, :decimal, precision: 32, scale: 16, default: 0
  end
end
