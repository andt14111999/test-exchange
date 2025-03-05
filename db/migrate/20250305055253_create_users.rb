# frozen_string_literal: true

class CreateUsers < ActiveRecord::Migration[8.0]
  def change
    create_table :users do |t|
      t.string :email, null: false
      t.string :display_name
      t.string :avatar_url
      t.string :role, default: 'user'
      t.boolean :phone_verified, null: false, default: false
      t.boolean :document_verified, null: false, default: false
      t.integer :kyc_level, default: 0
      t.string :status, default: 'active'

      t.timestamps
    end

    add_index :users, :email, unique: true
  end
end
