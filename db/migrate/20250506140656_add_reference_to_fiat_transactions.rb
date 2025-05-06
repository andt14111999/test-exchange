class AddReferenceToFiatTransactions < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    add_column :fiat_transactions, :reference, :string
    add_index :fiat_transactions, :reference, algorithm: :concurrently
  end
end
