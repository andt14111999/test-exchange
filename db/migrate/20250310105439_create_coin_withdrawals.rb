# frozen_string_literal: true

class CreateCoinWithdrawals < ActiveRecord::Migration[8.0]
  def change
    create_table :coin_withdrawals do |t|
      t.datetime :approve_scheduled_at
      t.string :coin_address
      t.decimal :coin_amount, precision: 24, scale: 8
      t.string :coin_currency, limit: 14
      t.decimal :coin_fee, precision: 24, scale: 8, default: 0.0
      t.string :coin_layer
      t.bigint :destination_tag
      t.string :explanation
      t.datetime :processed_at
      t.string :receiver_email
      t.string :receiver_phone_number
      t.string :receiver_username
      t.integer :receiving_coin_account_id
      t.string :status
      t.string :tx_hash
      t.datetime :tx_hash_arrived_at
      t.boolean :vpn, null: false, default: false
      t.references :user
      t.references :withdrawable, polymorphic: true, index: { unique: true }

      t.timestamps

      t.index [ :coin_address ],
        name: 'index_on_coin_address_of_coin_withdrawals_if_lightning',
        unique: true,
        where: "coin_layer = 'lightning'"

      t.index %i[coin_currency coin_layer]

      t.index %i[status approve_scheduled_at],
        name: 'index_coin_withdrawals_for_delayed_approval',
        where: "status = 'delayed_approval'"

      t.index %i[status updated_at],
        name: 'index_coin_withdrawals_for_stuck_recovery',
        where: "status IN ('prepared', 'verifying', 'verified')"

      t.index %i[user_id coin_currency id]
      t.index %i[user_id created_at]
      t.index [ :vpn ],
        name: 'index_coin_withdrawals_on_vpn',
        where: 'vpn = true'
    end
  end
end
