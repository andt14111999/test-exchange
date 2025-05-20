class AddColumnsToTrade < ActiveRecord::Migration[8.0]
  def change
    add_column :trades, :dispute_resolution, :string
    add_column :trades, :trade_memo, :string
  end
end
