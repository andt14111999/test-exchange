# frozen_string_literal: true

class CreateTrades < ActiveRecord::Migration[8.0]
  def change
    create_table :trades do |t|
      t.string :ref, null: false
      t.references :buyer, null: false, foreign_key: { to_table: :users }
      t.references :seller, null: false, foreign_key: { to_table: :users }
      t.references :offer, null: false, foreign_key: true
      t.string :coin_currency, null: false
      t.string :fiat_currency, null: false
      t.decimal :coin_amount, precision: 32, scale: 16, null: false
      t.decimal :fiat_amount, precision: 32, scale: 16, null: false
      t.decimal :price, precision: 32, scale: 16, null: false
      t.decimal :fee_ratio, precision: 10, scale: 4, null: false
      t.decimal :coin_trading_fee, precision: 32, scale: 16, null: false
      t.string :payment_method, null: false
      t.jsonb :payment_details, default: {}
      t.string :taker_side, null: false

      # Status
      t.string :status, null: false, default: 'awaiting'

      # Timestamps
      t.datetime :paid_at
      t.datetime :released_at
      t.datetime :expired_at
      t.datetime :cancelled_at
      t.datetime :disputed_at

      # Proof & dispute
      t.jsonb :payment_receipt_details, default: {}
      t.boolean :has_payment_proof, default: false
      t.string :payment_proof_status
      t.text :dispute_reason

      # Price data
      t.decimal :open_coin_price, precision: 32, scale: 16
      t.decimal :close_coin_price, precision: 32, scale: 16
      t.decimal :release_coin_price, precision: 32, scale: 16

      # Fields for fiat token
      t.references :fiat_token_deposit, foreign_key: { to_table: :fiat_deposits }
      t.references :fiat_token_withdrawal, foreign_key: { to_table: :fiat_withdrawals }

      t.timestamps

      t.index :ref, unique: true
      t.index [ :buyer_id, :status ]
      t.index [ :seller_id, :status ]
    end
  end
end
