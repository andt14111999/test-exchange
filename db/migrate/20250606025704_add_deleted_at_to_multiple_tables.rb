class AddDeletedAtToMultipleTables < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    tables = %w[
      amm_orders amm_pools amm_positions balance_locks balance_lock_operations
      bank_accounts coin_accounts coin_deposit_operations coin_deposits
      coin_transactions coin_withdrawals coin_withdrawal_operations
      fiat_accounts fiat_deposits fiat_transactions fiat_withdrawals
      merchant_escrows merchant_escrow_operations offers payment_methods
      ticks trades users
    ]

    tables.each do |table|
      add_column table, :deleted_at, :datetime
      add_index table, :deleted_at, algorithm: :concurrently
    end
  end
end
