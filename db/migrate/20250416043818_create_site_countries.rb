# frozen_string_literal: true

class CreateSiteCountries < ActiveRecord::Migration[8.0]
  def change
    create_table :site_countries do |t|
      t.string :code, null: false
      t.string :name, null: false
      t.boolean :enabled, default: true

      t.timestamps
    end

    add_index :site_countries, :code, unique: true
  end
end
