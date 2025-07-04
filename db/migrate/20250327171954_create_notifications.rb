# frozen_string_literal: true

class CreateNotifications < ActiveRecord::Migration[8.0]
  def change
    create_table :notifications do |t|
      t.references :user, null: false, foreign_key: true
      t.string :title, null: false
      t.text :content, null: false
      t.string :notification_type, null: false
      t.boolean :read, default: false
      t.boolean :delivered, default: false

      t.timestamps
    end

    add_index :notifications, [ :user_id, :created_at ]
  end
end
