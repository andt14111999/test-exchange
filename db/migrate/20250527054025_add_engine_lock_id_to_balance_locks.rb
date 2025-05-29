class AddEngineLockIdToBalanceLocks < ActiveRecord::Migration[8.0]
  def change
    add_column :balance_locks, :engine_lock_id, :string
  end
end
