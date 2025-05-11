class CreateCoinInternalTransferOperations < ActiveRecord::Migration[8.0]
  def change
    create_table :coin_internal_transfer_operations do |t|
      t.references :coin_withdrawal, null: false, foreign_key: true
      t.references :sender, null: false, foreign_key: { to_table: :users }
      t.references :receiver, null: false, foreign_key: { to_table: :users }
      t.string :coin_currency, null: false
      t.decimal :coin_amount, precision: 36, scale: 18, null: false
      t.string :status, default: 'pending', null: false
      t.string :status_explanation
      t.timestamps
    end

    add_index :coin_internal_transfer_operations, :status
    add_index :coin_internal_transfer_operations, :coin_currency
  end
end
