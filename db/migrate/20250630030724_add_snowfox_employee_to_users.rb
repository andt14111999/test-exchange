class AddSnowfoxEmployeeToUsers < ActiveRecord::Migration[8.0]
  def change
    add_column :users, :snowfox_employee, :boolean, default: false, null: false
  end
end
