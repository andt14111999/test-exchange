class CreateOffers < ActiveRecord::Migration[8.0]
  def change
    create_table :offers do |t|
      t.references :user, null: false, foreign_key: true
      t.string :offer_type, null: false
      t.string :coin_currency, null: false
      t.string :currency, null: false
      t.decimal :price, precision: 32, scale: 16, null: false
      t.decimal :min_amount, precision: 32, scale: 16, null: false
      t.decimal :max_amount, precision: 32, scale: 16, null: false
      t.decimal :total_amount, precision: 32, scale: 16, null: false
      t.references :payment_method, foreign_key: true
      t.integer :payment_time, null: false, default: 30
      t.jsonb :payment_details, default: {}
      t.string :country_code, null: false
      t.boolean :disabled, default: false
      t.boolean :deleted, default: false
      t.boolean :automatic, default: false
      t.boolean :online, default: true
      t.text :terms_of_trade
      t.string :disable_reason
      t.decimal :margin, precision: 10, scale: 4
      t.decimal :fixed_coin_price, precision: 32, scale: 16
      t.string :bank_names, array: true, default: []

      t.timestamps

      t.index [ :user_id, :offer_type ]
      t.index [ :coin_currency, :currency, :offer_type, :disabled, :deleted ]
      t.index :country_code
    end
  end
end
