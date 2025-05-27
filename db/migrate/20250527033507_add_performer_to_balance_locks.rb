class AddPerformerToBalanceLocks < ActiveRecord::Migration[8.0]
  def change
    add_column :balance_locks, :performer, :string
  end
end
