class AddDetailsToFiatTransactions < ActiveRecord::Migration[8.0]
  def change
    add_column :fiat_transactions, :details, :jsonb, default: {}
  end
end
