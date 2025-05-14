class CreateApiKeys < ActiveRecord::Migration[7.0]
  def change
    create_table :api_keys do |t|
      t.references :user, null: false, foreign_key: true
      t.string :name, null: false
      t.string :access_key, null: false
      t.string :encrypted_secret_key
      t.string :encrypted_secret_key_iv
      t.string :encrypted_secret_key_salt
      t.datetime :last_used_at
      t.datetime :revoked_at

      t.timestamps
    end
    add_index :api_keys, :access_key, unique: true
    add_index :api_keys, :encrypted_secret_key_iv, unique: true
  end
end
