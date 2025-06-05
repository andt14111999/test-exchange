# frozen_string_literal: true

class CreateKafkaEvents < ActiveRecord::Migration[8.0]
  def change
    create_table :kafka_events do |t|
      t.string :event_id, null: false
      t.string :topic_name, null: false
      t.jsonb :payload
      t.datetime :processed_at
      t.string :status
      t.string :error_message

      t.timestamps
    end

    add_index :kafka_events, [ :event_id, :topic_name ], unique: true
    add_index :kafka_events, :status
  end
end
