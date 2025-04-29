# frozen_string_literal: true

class CreateBankAccounts < ActiveRecord::Migration[8.0]
  def change
    create_table :bank_accounts do |t|
      t.references :user, null: false, foreign_key: true
      t.string :bank_name, null: false
      t.string :account_name, null: false
      t.string :account_number, null: false
      t.string :branch
      t.string :country_code, null: false
      t.boolean :verified, default: false
      t.boolean :is_primary, default: false

      t.timestamps

      t.index [ :user_id, :bank_name, :account_number ], unique: true
      t.index :country_code
    end
  end
end
