class AddStatusAndErrorMessageToModels < ActiveRecord::Migration[8.0]
  def change
    # Add status column to coin_transactions
    add_column :coin_transactions, :status, :string, default: 'pending'

    # Add status column to offers
    add_column :offers, :status, :string, default: 'active'

    # Add error_message to all models that need it
    add_column :coin_transactions, :error_message, :text
    add_column :amm_pools, :error_message, :text
    add_column :merchant_escrows, :error_message, :text
    add_column :trades, :error_message, :text
    add_column :offers, :error_message, :text
    add_column :balance_locks, :error_message, :text
  end
end
