class CreateAccessDevices < ActiveRecord::Migration[8.0]
  def change
    create_table :access_devices, id: :serial do |t|
      t.references :user, null: false, foreign_key: true, type: :integer
      t.string :device_uuid_hash, null: false
      t.jsonb :details, null: false
      t.boolean :first_device, default: false, null: false
      t.boolean :trusted, default: false, null: false

      t.timestamps precision: nil, null: false

      t.index [ :user_id, :device_uuid_hash ], unique: true, name: 'index_access_devices_on_user_id_and_device_uuid_hash'
    end
  end
end
