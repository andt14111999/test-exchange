# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# This file is the source Rails uses to define your schema when running `bin/rails
# db:schema:load`. When creating a new database, `bin/rails db:schema:load` tends to
# be faster and is potentially less error prone than running all of your
# migrations from scratch. Old migrations may fail to apply correctly if those
# migrations use external dependencies or application code.
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema[8.0].define(version: 2025_03_18_020803) do
  # These are extensions that must be enabled in order to support this database
  enable_extension "pg_catalog.plpgsql"

  create_table "active_admin_comments", force: :cascade do |t|
    t.string "namespace"
    t.text "body"
    t.string "resource_type"
    t.bigint "resource_id"
    t.string "author_type"
    t.bigint "author_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["author_type", "author_id"], name: "index_active_admin_comments_on_author"
    t.index ["namespace"], name: "index_active_admin_comments_on_namespace"
    t.index ["resource_type", "resource_id"], name: "index_active_admin_comments_on_resource"
  end

  create_table "admin_users", force: :cascade do |t|
    t.string "email", default: "", null: false
    t.string "encrypted_password", default: "", null: false
    t.string "reset_password_token"
    t.datetime "reset_password_sent_at"
    t.datetime "remember_created_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "fullname"
    t.string "roles", default: ""
    t.boolean "authenticator_enabled", default: false, null: false
    t.string "authenticator_key"
    t.index ["email"], name: "index_admin_users_on_email", unique: true
    t.index ["reset_password_token"], name: "index_admin_users_on_reset_password_token", unique: true
  end

  create_table "coin_accounts", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "coin_currency", null: false
    t.string "layer", null: false
    t.decimal "balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "frozen_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.string "account_type", default: "deposit", null: false
    t.string "address"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id", "coin_currency", "layer"], name: "index_coin_accounts_on_user_id_and_coin_currency_and_layer", unique: true
    t.index ["user_id"], name: "index_coin_accounts_on_user_id"
  end

  create_table "coin_deposit_operations", force: :cascade do |t|
    t.bigint "coin_account_id"
    t.decimal "coin_amount", precision: 24, scale: 8
    t.string "coin_currency", limit: 5
    t.bigint "coin_deposit_id"
    t.decimal "coin_fee", precision: 24, scale: 8, default: "0.0"
    t.decimal "platform_fee", precision: 24, scale: 8, default: "0.0"
    t.string "tx_hash"
    t.integer "out_index"
    t.string "status"
    t.text "status_explanation"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["coin_account_id"], name: "index_coin_deposit_operations_on_coin_account_id"
    t.index ["coin_deposit_id"], name: "index_coin_deposit_operations_on_coin_deposit_id"
    t.index ["status"], name: "index_coin_deposit_operations_on_status"
    t.index ["tx_hash", "out_index"], name: "index_coin_deposit_operations_on_tx_hash_and_out_index", unique: true
    t.index ["tx_hash"], name: "index_coin_deposit_operations_on_tx_hash"
  end

  create_table "coin_deposits", force: :cascade do |t|
    t.bigint "user_id"
    t.bigint "coin_account_id"
    t.string "coin_currency", null: false
    t.decimal "coin_amount", precision: 32, scale: 16, null: false
    t.decimal "coin_fee", precision: 32, scale: 16, default: "0.0"
    t.string "tx_hash"
    t.integer "out_index", default: 0
    t.integer "confirmations_count", default: 0
    t.integer "required_confirmations_count"
    t.string "status", default: "pending"
    t.string "locked_reason"
    t.string "last_seen_ip"
    t.datetime "verified_at"
    t.jsonb "metadata"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["coin_account_id"], name: "index_coin_deposits_on_coin_account_id"
    t.index ["created_at"], name: "index_coin_deposits_on_created_at"
    t.index ["status"], name: "index_coin_deposits_on_status"
    t.index ["tx_hash", "out_index", "coin_currency", "coin_account_id"], name: "index_coin_deposits_on_tx_hash_and_related", unique: true
    t.index ["user_id"], name: "index_coin_deposits_on_user_id"
    t.index ["verified_at"], name: "index_coin_deposits_on_verified_at"
  end

  create_table "coin_transactions", force: :cascade do |t|
    t.decimal "amount", precision: 24, scale: 8
    t.bigint "coin_account_id"
    t.string "coin_currency", limit: 14
    t.string "operation_type"
    t.bigint "operation_id"
    t.decimal "snapshot_balance", precision: 24, scale: 8
    t.decimal "snapshot_frozen_balance", precision: 24, scale: 8
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["coin_account_id"], name: "index_coin_transactions_on_coin_account_id"
    t.index ["coin_currency"], name: "index_coin_transactions_on_coin_currency"
    t.index ["created_at"], name: "index_coin_transactions_on_created_at"
    t.index ["operation_type", "operation_id"], name: "index_coin_transactions_on_operation"
    t.index ["operation_type", "operation_id"], name: "index_coin_transactions_on_operation_type_and_operation_id"
  end

  create_table "coin_withdrawal_operations", id: :serial, force: :cascade do |t|
    t.decimal "coin_amount", precision: 24, scale: 8
    t.string "coin_currency", limit: 14
    t.decimal "coin_fee", precision: 24, scale: 8, default: "0.0"
    t.bigint "coin_withdrawal_id"
    t.datetime "scheduled_at"
    t.string "status"
    t.text "status_explanation"
    t.string "tx_hash"
    t.jsonb "withdrawal_data"
    t.string "withdrawal_status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["coin_withdrawal_id"], name: "index_coin_withdrawal_operations_on_coin_withdrawal_id"
    t.index ["created_at"], name: "index_coin_withdrawal_operations_on_created_at"
    t.index ["scheduled_at"], name: "index_coin_withdrawal_operations_on_scheduled_at"
    t.index ["status"], name: "index_coin_withdrawal_operations_on_status"
    t.index ["tx_hash"], name: "index_coin_withdrawal_operations_on_tx_hash"
    t.index ["withdrawal_status"], name: "index_coin_withdrawal_operations_on_withdrawal_status"
  end

  create_table "coin_withdrawals", force: :cascade do |t|
    t.datetime "approve_scheduled_at"
    t.string "coin_address"
    t.decimal "coin_amount", precision: 24, scale: 8
    t.string "coin_currency", limit: 14
    t.decimal "coin_fee", precision: 24, scale: 8, default: "0.0"
    t.string "coin_layer"
    t.bigint "destination_tag"
    t.string "explanation"
    t.datetime "processed_at"
    t.string "receiver_email"
    t.string "receiver_phone_number"
    t.string "receiver_username"
    t.integer "receiving_coin_account_id"
    t.string "status"
    t.string "tx_hash"
    t.datetime "tx_hash_arrived_at"
    t.boolean "vpn", default: false, null: false
    t.bigint "user_id"
    t.string "withdrawable_type"
    t.bigint "withdrawable_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["coin_address"], name: "index_on_coin_address_of_coin_withdrawals_if_lightning", unique: true, where: "((coin_layer)::text = 'lightning'::text)"
    t.index ["coin_currency", "coin_layer"], name: "index_coin_withdrawals_on_coin_currency_and_coin_layer"
    t.index ["status", "approve_scheduled_at"], name: "index_coin_withdrawals_for_delayed_approval", where: "((status)::text = 'delayed_approval'::text)"
    t.index ["status", "updated_at"], name: "index_coin_withdrawals_for_stuck_recovery", where: "((status)::text = ANY (ARRAY[('prepared'::character varying)::text, ('verifying'::character varying)::text, ('verified'::character varying)::text]))"
    t.index ["user_id", "coin_currency", "id"], name: "index_coin_withdrawals_on_user_id_and_coin_currency_and_id"
    t.index ["user_id", "created_at"], name: "index_coin_withdrawals_on_user_id_and_created_at"
    t.index ["user_id"], name: "index_coin_withdrawals_on_user_id"
    t.index ["vpn"], name: "index_coin_withdrawals_on_vpn", where: "(vpn = true)"
    t.index ["withdrawable_type", "withdrawable_id"], name: "index_coin_withdrawals_on_withdrawable", unique: true
  end

  create_table "fiat_accounts", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "currency", null: false
    t.decimal "balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "frozen_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id", "currency"], name: "index_fiat_accounts_on_user_id_and_currency", unique: true
    t.index ["user_id"], name: "index_fiat_accounts_on_user_id"
  end

  create_table "fiat_transactions", force: :cascade do |t|
    t.decimal "amount", precision: 32, scale: 16, null: false
    t.bigint "fiat_account_id", null: false
    t.string "currency", limit: 14, null: false
    t.string "operation_type"
    t.bigint "operation_id"
    t.decimal "snapshot_balance", precision: 32, scale: 16
    t.decimal "snapshot_frozen_balance", precision: 32, scale: 16
    t.string "transaction_type"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["created_at"], name: "index_fiat_transactions_on_created_at"
    t.index ["currency"], name: "index_fiat_transactions_on_currency"
    t.index ["fiat_account_id"], name: "index_fiat_transactions_on_fiat_account_id"
    t.index ["operation_type", "operation_id"], name: "index_fiat_transactions_on_operation"
    t.index ["operation_type", "operation_id"], name: "index_fiat_transactions_on_operation_type_and_operation_id"
  end

  create_table "merchant_escrow_operations", force: :cascade do |t|
    t.bigint "merchant_escrow_id", null: false
    t.bigint "usdt_account_id", null: false
    t.bigint "fiat_account_id", null: false
    t.decimal "usdt_amount", precision: 32, scale: 16, null: false
    t.decimal "fiat_amount", precision: 32, scale: 16, null: false
    t.string "fiat_currency", null: false
    t.string "operation_type", null: false
    t.string "status", null: false
    t.text "status_explanation"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["created_at"], name: "index_merchant_escrow_operations_on_created_at"
    t.index ["fiat_account_id"], name: "index_merchant_escrow_operations_on_fiat_account_id"
    t.index ["merchant_escrow_id"], name: "index_merchant_escrow_operations_on_merchant_escrow_id"
    t.index ["operation_type"], name: "index_merchant_escrow_operations_on_operation_type"
    t.index ["status"], name: "index_merchant_escrow_operations_on_status"
    t.index ["usdt_account_id"], name: "index_merchant_escrow_operations_on_usdt_account_id"
  end

  create_table "merchant_escrows", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.bigint "usdt_account_id", null: false
    t.bigint "fiat_account_id", null: false
    t.decimal "usdt_amount", precision: 32, scale: 16, null: false
    t.decimal "fiat_amount", precision: 32, scale: 16, null: false
    t.string "fiat_currency", null: false
    t.string "status", null: false
    t.datetime "completed_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["created_at"], name: "index_merchant_escrows_on_created_at"
    t.index ["fiat_account_id"], name: "index_merchant_escrows_on_fiat_account_id"
    t.index ["fiat_currency"], name: "index_merchant_escrows_on_fiat_currency"
    t.index ["status"], name: "index_merchant_escrows_on_status"
    t.index ["usdt_account_id"], name: "index_merchant_escrows_on_usdt_account_id"
    t.index ["user_id"], name: "index_merchant_escrows_on_user_id"
  end

  create_table "settings", force: :cascade do |t|
    t.string "var", null: false
    t.text "value"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["var"], name: "index_settings_on_var", unique: true
  end

  create_table "social_accounts", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "provider", null: false
    t.string "provider_user_id", null: false
    t.string "email", null: false
    t.string "name"
    t.string "access_token"
    t.string "refresh_token"
    t.datetime "token_expires_at"
    t.string "avatar_url"
    t.jsonb "profile_data"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["provider", "provider_user_id"], name: "index_social_accounts_on_provider_and_provider_user_id", unique: true
    t.index ["user_id"], name: "index_social_accounts_on_user_id"
  end

  create_table "users", force: :cascade do |t|
    t.string "email", null: false
    t.string "display_name"
    t.string "avatar_url"
    t.string "role", default: "user"
    t.boolean "phone_verified", default: false, null: false
    t.boolean "document_verified", default: false, null: false
    t.integer "kyc_level", default: 0
    t.string "status", default: "active"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["email"], name: "index_users_on_email", unique: true
  end

  create_table "versions", force: :cascade do |t|
    t.string "whodunnit"
    t.datetime "created_at"
    t.bigint "item_id", null: false
    t.string "item_type", null: false
    t.string "event", null: false
    t.text "object"
    t.index ["item_type", "item_id"], name: "index_versions_on_item_type_and_item_id"
  end

  add_foreign_key "coin_accounts", "users"
  add_foreign_key "coin_deposit_operations", "coin_accounts"
  add_foreign_key "coin_deposit_operations", "coin_deposits"
  add_foreign_key "coin_deposits", "coin_accounts"
  add_foreign_key "coin_deposits", "users"
  add_foreign_key "coin_transactions", "coin_accounts"
  add_foreign_key "fiat_accounts", "users"
  add_foreign_key "fiat_transactions", "fiat_accounts"
  add_foreign_key "merchant_escrow_operations", "coin_accounts", column: "usdt_account_id"
  add_foreign_key "merchant_escrow_operations", "fiat_accounts"
  add_foreign_key "merchant_escrow_operations", "merchant_escrows"
  add_foreign_key "merchant_escrows", "coin_accounts", column: "usdt_account_id"
  add_foreign_key "merchant_escrows", "fiat_accounts"
  add_foreign_key "merchant_escrows", "users"
  add_foreign_key "social_accounts", "users"
end
