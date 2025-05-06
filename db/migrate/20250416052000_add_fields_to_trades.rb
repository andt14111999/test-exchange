class AddFieldsToTrades < ActiveRecord::Migration[8.0]
  def change
    add_column :trades, :amount, :decimal
    add_column :trades, :token_amount, :decimal
  end
end
