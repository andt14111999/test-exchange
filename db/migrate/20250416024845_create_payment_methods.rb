class CreatePaymentMethods < ActiveRecord::Migration[8.0]
  def change
    create_table :payment_methods do |t|
      t.string :name, null: false
      t.string :display_name, null: false
      t.text :description
      t.string :country_code, null: false
      t.boolean :enabled, default: true
      t.jsonb :fields_required, default: {}
      t.string :icon_url

      t.timestamps

      t.index :name, unique: true
      t.index :country_code
    end
  end
end
