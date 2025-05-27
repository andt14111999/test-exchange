class CreateBalanceLockOperations < ActiveRecord::Migration[8.0]
  def change
    create_table :balance_lock_operations do |t|
      t.references :balance_lock, null: false, foreign_key: true
      t.string :operation_type, null: false
      t.string :status, null: false, default: 'pending'
      t.text :status_explanation

      t.timestamps
    end

    add_index :balance_lock_operations, :operation_type
    add_index :balance_lock_operations, :status
  end
end
