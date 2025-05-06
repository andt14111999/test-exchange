class CreateMessages < ActiveRecord::Migration[8.0]
  def change
    create_table :messages do |t|
      t.references :trade, null: false, foreign_key: true
      t.references :user, null: false, foreign_key: true
      t.text :body, null: false
      t.boolean :is_receipt_proof, default: false
      t.boolean :is_system, default: false

      t.timestamps

      t.index :is_receipt_proof
      t.index :is_system
    end
  end
end
