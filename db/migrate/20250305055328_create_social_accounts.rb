# frozen_string_literal: true

class CreateSocialAccounts < ActiveRecord::Migration[8.0]
  def change
    create_table :social_accounts do |t|
      t.references :user, null: false, foreign_key: true
      t.string :provider, null: false
      t.string :provider_user_id, null: false
      t.string :email, null: false
      t.string :name
      t.string :access_token
      t.string :refresh_token
      t.datetime :token_expires_at
      t.string :avatar_url
      t.jsonb :profile_data

      t.timestamps
    end

    add_index :social_accounts, %i[provider provider_user_id], unique: true
  end
end
