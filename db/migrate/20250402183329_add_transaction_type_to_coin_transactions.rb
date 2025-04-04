class AddTransactionTypeToCoinTransactions < ActiveRecord::Migration[8.0]
  def change
    add_column :coin_transactions, :transaction_type, :string, null: false, default: 'transfer'
  end
end
