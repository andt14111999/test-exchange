class CreateCoinSettings < ActiveRecord::Migration[8.0]
  def change
    create_table :coin_settings do |t|
      t.string :currency
      t.boolean :deposit_enabled
      t.boolean :withdraw_enabled
      t.boolean :swap_enabled
      t.jsonb :layers

      t.timestamps
    end
  end
end
