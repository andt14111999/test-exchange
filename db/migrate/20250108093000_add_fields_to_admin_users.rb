# frozen_string_literal: true

class AddFieldsToAdminUsers < ActiveRecord::Migration[7.0]
  def change
    add_column :admin_users, :fullname, :string
    add_column :admin_users, :roles, :string, default: ''
    add_column :admin_users, :authenticator_enabled, :boolean, default: false, null: false
    add_column :admin_users, :authenticator_key, :string
  end
end
