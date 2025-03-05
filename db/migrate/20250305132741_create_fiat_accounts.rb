# frozen_string_literal: true

class CreateFiatAccounts < ActiveRecord::Migration[7.0]
  def change
    create_table :fiat_accounts do |t|
      t.references :user, null: false, foreign_key: true
      t.string :currency, null: false
      t.decimal :balance, null: false, default: 0, precision: 32, scale: 16
      t.decimal :frozen_balance, null: false, default: 0, precision: 32, scale: 16
      t.decimal :total_balance, null: false, default: 0, precision: 32, scale: 16
      t.decimal :available_balance, null: false, default: 0, precision: 32, scale: 16

      t.timestamps

      t.index %i[user_id currency], unique: true
    end
  end
end
