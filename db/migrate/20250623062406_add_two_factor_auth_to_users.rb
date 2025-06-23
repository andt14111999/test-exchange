class AddTwoFactorAuthToUsers < ActiveRecord::Migration[8.0]
  def change
    add_column :users, :authenticator_enabled, :boolean, default: false, null: false
    add_column :users, :authenticator_key, :string
  end
end
