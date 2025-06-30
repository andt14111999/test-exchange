class AddDeactivatedToAdminUsers < ActiveRecord::Migration[8.0]
  def change
    add_column :admin_users, :deactivated, :boolean, default: false, null: false
  end
end
