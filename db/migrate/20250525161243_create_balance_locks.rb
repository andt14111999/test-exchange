class CreateBalanceLocks < ActiveRecord::Migration[8.0]
  def change
    create_table :balance_locks do |t|
      t.references :user, null: false, foreign_key: true
      t.jsonb :locked_balances, null: false, default: {}
      t.string :status, null: false
      t.text :reason
      t.datetime :locked_at
      t.datetime :unlocked_at

      t.timestamps
    end

    add_index :balance_locks, :status
  end
end
