# frozen_string_literal: true

class CreateCoinDeposits < ActiveRecord::Migration[7.0]
  def change
    create_table :coin_deposits do |t|
      t.references :user, foreign_key: true
      t.references :coin_account, foreign_key: true
      t.string :coin_currency, null: false
      t.decimal :coin_amount, precision: 32, scale: 16, null: false
      t.decimal :coin_fee, precision: 32, scale: 16, default: 0
      t.string :tx_hash
      t.integer :out_index, default: 0
      t.integer :confirmations_count, default: 0
      t.integer :required_confirmations_count
      t.string :status, default: 'pending'
      t.string :locked_reason
      t.string :last_seen_ip
      t.datetime :verified_at
      t.jsonb :metadata

      t.timestamps

      t.index %i[tx_hash out_index coin_currency coin_account_id],
        unique: true, name: 'index_coin_deposits_on_tx_hash_and_related'
      t.index :status
      t.index :created_at
      t.index :verified_at
    end
  end
end
