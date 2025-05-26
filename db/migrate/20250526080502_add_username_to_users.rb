class AddUsernameToUsers < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    add_column :users, :username, :string, limit: 20
    add_index :users, :username, unique: true, algorithm: :concurrently
  end
end
