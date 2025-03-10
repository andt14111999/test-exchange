# frozen_string_literal: true

class CreateCoinAccounts < ActiveRecord::Migration[7.0]
  def change
    create_table :coin_accounts do |t|
      t.references :user, null: false, foreign_key: true
      t.string :coin_type, null: false
      t.string :layer, null: false
      t.decimal :balance, null: false, default: 0, precision: 32, scale: 16
      t.decimal :frozen_balance, null: false, default: 0, precision: 32, scale: 16
      t.string :account_type, null: false, default: 'deposit'
      t.string :address

      t.timestamps

      t.index %i[user_id coin_type layer], unique: true
    end
  end
end
