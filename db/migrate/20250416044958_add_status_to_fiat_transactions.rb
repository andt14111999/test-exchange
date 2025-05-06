class AddStatusToFiatTransactions < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    add_column :fiat_transactions, :status, :string, default: 'pending', null: false
    add_index :fiat_transactions, :status, algorithm: :concurrently
  end
end
