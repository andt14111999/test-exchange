class AddScheduleFieldsToOffers < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    add_column :offers, :schedule_start_time, :datetime
    add_column :offers, :schedule_end_time, :datetime

    add_index :offers, :schedule_start_time, algorithm: :concurrently
    add_index :offers, :schedule_end_time, algorithm: :concurrently
  end
end
