class CreateBanks < ActiveRecord::Migration[8.0]
  def change
    create_table :banks do |t|
      t.string :name
      t.string :code
      t.string :bin
      t.string :short_name
      t.string :logo
      t.boolean :transfer_supported
      t.boolean :lookup_supported
      t.integer :support
      t.boolean :is_transfer
      t.string :swift_code
      t.references :country, null: false, foreign_key: true

      t.timestamps
    end
  end
end
