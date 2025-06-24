class AddProcessMessageToKafkaEvents < ActiveRecord::Migration[8.0]
  def change
    add_column :kafka_events, :process_message, :text
  end
end
