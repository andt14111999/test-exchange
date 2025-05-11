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

ActiveRecord::Schema[8.0].define(version: 2025_05_12_103553) do
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

  create_table "amm_orders", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.bigint "amm_pool_id", null: false
    t.string "identifier", null: false
    t.boolean "zero_for_one", default: true, null: false
    t.decimal "amount_specified", precision: 36, scale: 18, default: "0.0", null: false
    t.decimal "amount_estimated", precision: 36, scale: 18, default: "0.0", null: false
    t.decimal "amount_actual", precision: 36, scale: 18, default: "0.0", null: false
    t.integer "before_tick_index"
    t.integer "after_tick_index"
    t.jsonb "fees", default: {}, null: false
    t.string "status", default: "pending", null: false
    t.string "error_message", default: ""
    t.float "slippage", default: 0.01, null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.decimal "amount_received", precision: 32, scale: 16, default: "0.0"
    t.index ["amm_pool_id"], name: "index_amm_orders_on_amm_pool_id"
    t.index ["identifier"], name: "index_amm_orders_on_identifier", unique: true
    t.index ["user_id"], name: "index_amm_orders_on_user_id"
  end

  create_table "amm_pools", force: :cascade do |t|
    t.string "pair", null: false
    t.string "token0", null: false
    t.string "token1", null: false
    t.integer "tick_spacing", null: false
    t.decimal "fee_percentage", precision: 10, scale: 6, null: false
    t.decimal "fee_protocol_percentage", precision: 10, scale: 6, default: "0.0"
    t.integer "current_tick", default: 0
    t.decimal "sqrt_price", precision: 36, scale: 18, default: "1.0"
    t.decimal "price", precision: 36, scale: 18, default: "1.0"
    t.decimal "liquidity", precision: 36, scale: 18, default: "0.0"
    t.decimal "fee_growth_global0", precision: 36, scale: 18, default: "0.0"
    t.decimal "fee_growth_global1", precision: 36, scale: 18, default: "0.0"
    t.decimal "protocol_fees0", precision: 36, scale: 18, default: "0.0"
    t.decimal "protocol_fees1", precision: 36, scale: 18, default: "0.0"
    t.decimal "volume_token0", precision: 36, scale: 18, default: "0.0"
    t.decimal "volume_token1", precision: 36, scale: 18, default: "0.0"
    t.decimal "volume_usd", precision: 36, scale: 18, default: "0.0"
    t.integer "tx_count", default: 0
    t.decimal "total_value_locked_token0", precision: 36, scale: 18, default: "0.0"
    t.decimal "total_value_locked_token1", precision: 36, scale: 18, default: "0.0"
    t.string "status", default: "pending"
    t.string "status_explanation", default: ""
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.decimal "init_price", precision: 36, scale: 18
    t.index ["pair"], name: "index_amm_pools_on_pair", unique: true
    t.index ["status"], name: "index_amm_pools_on_status"
    t.index ["token0", "token1", "fee_percentage"], name: "index_amm_pools_on_token0_and_token1_and_fee_percentage", unique: true
  end

  create_table "amm_positions", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.bigint "amm_pool_id", null: false
    t.string "identifier", null: false
    t.string "status", default: "pending", null: false
    t.decimal "liquidity", default: "0.0", null: false
    t.decimal "slippage", default: "1.0", null: false
    t.integer "tick_lower_index", null: false
    t.integer "tick_upper_index", null: false
    t.decimal "amount0", default: "0.0", null: false
    t.decimal "amount1", default: "0.0", null: false
    t.decimal "amount0_initial", default: "0.0", null: false
    t.decimal "amount1_initial", default: "0.0", null: false
    t.decimal "fee_growth_inside0_last", default: "0.0", null: false
    t.decimal "fee_growth_inside1_last", default: "0.0", null: false
    t.decimal "tokens_owed0", default: "0.0", null: false
    t.decimal "tokens_owed1", default: "0.0", null: false
    t.decimal "fee_collected0", default: "0.0", null: false
    t.decimal "fee_collected1", default: "0.0", null: false
    t.string "error_message"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["amm_pool_id"], name: "index_amm_positions_on_amm_pool_id"
    t.index ["identifier"], name: "index_amm_positions_on_identifier", unique: true
    t.index ["status"], name: "index_amm_positions_on_status"
    t.index ["user_id"], name: "index_amm_positions_on_user_id"
  end

  create_table "bank_accounts", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "bank_name", null: false
    t.string "account_name", null: false
    t.string "account_number", null: false
    t.string "branch"
    t.string "country_code", null: false
    t.boolean "verified", default: false
    t.boolean "is_primary", default: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["country_code"], name: "index_bank_accounts_on_country_code"
    t.index ["user_id", "bank_name", "account_number"], name: "idx_on_user_id_bank_name_account_number_9dd9c83454", unique: true
    t.index ["user_id"], name: "index_bank_accounts_on_user_id"
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

  create_table "coin_internal_transfer_operations", force: :cascade do |t|
    t.bigint "coin_withdrawal_id", null: false
    t.bigint "sender_id", null: false
    t.bigint "receiver_id", null: false
    t.string "coin_currency", null: false
    t.decimal "coin_amount", precision: 36, scale: 18, null: false
    t.string "status", default: "pending", null: false
    t.string "status_explanation"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.decimal "coin_fee", precision: 36, scale: 18, default: "0.0"
    t.index ["coin_currency"], name: "index_coin_internal_transfer_operations_on_coin_currency"
    t.index ["coin_withdrawal_id"], name: "index_coin_internal_transfer_operations_on_coin_withdrawal_id"
    t.index ["receiver_id"], name: "index_coin_internal_transfer_operations_on_receiver_id"
    t.index ["sender_id"], name: "index_coin_internal_transfer_operations_on_sender_id"
    t.index ["status"], name: "index_coin_internal_transfer_operations_on_status"
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
    t.string "transaction_type", default: "transfer", null: false
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
    t.string "country_code"
    t.string "frozen_reason"
    t.index ["country_code"], name: "index_fiat_accounts_on_country_code"
    t.index ["user_id", "currency"], name: "index_fiat_accounts_on_user_id_and_currency", unique: true
    t.index ["user_id"], name: "index_fiat_accounts_on_user_id"
  end

  create_table "fiat_deposits", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.bigint "fiat_account_id", null: false
    t.string "currency", null: false
    t.string "country_code", null: false
    t.decimal "fiat_amount", precision: 32, scale: 16, null: false
    t.decimal "original_fiat_amount", precision: 32, scale: 16
    t.decimal "deposit_fee", precision: 32, scale: 16, default: "0.0"
    t.string "explorer_ref"
    t.string "memo", null: false
    t.jsonb "fiat_deposit_details", default: {}
    t.string "ownership_proof_url"
    t.string "sender_name"
    t.string "sender_account_number"
    t.string "status", default: "awaiting", null: false
    t.string "cancel_reason"
    t.datetime "processed_at"
    t.datetime "cancelled_at"
    t.datetime "money_sent_at"
    t.string "payable_type"
    t.bigint "payable_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "verification_attempts", default: 0
    t.datetime "verification_last_attempt_at"
    t.jsonb "bank_response_data", default: {}
    t.string "payment_proof_url"
    t.text "payment_description"
    t.index ["country_code"], name: "index_fiat_deposits_on_country_code"
    t.index ["explorer_ref"], name: "index_fiat_deposits_on_explorer_ref"
    t.index ["fiat_account_id"], name: "index_fiat_deposits_on_fiat_account_id"
    t.index ["memo"], name: "index_fiat_deposits_on_memo", unique: true
    t.index ["money_sent_at"], name: "index_fiat_deposits_on_money_sent_at"
    t.index ["payable_type", "payable_id"], name: "index_fiat_deposits_on_payable_type_and_payable_id"
    t.index ["status", "created_at"], name: "index_fiat_deposits_on_status_and_created_at"
    t.index ["status"], name: "index_fiat_deposits_on_status"
    t.index ["user_id", "created_at"], name: "index_fiat_deposits_on_user_id_and_created_at"
    t.index ["user_id", "status"], name: "index_fiat_deposits_on_user_id_and_status"
    t.index ["user_id"], name: "index_fiat_deposits_on_user_id"
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
    t.string "status", default: "pending", null: false
    t.jsonb "details", default: {}
    t.string "reference"
    t.index ["created_at"], name: "index_fiat_transactions_on_created_at"
    t.index ["currency"], name: "index_fiat_transactions_on_currency"
    t.index ["fiat_account_id"], name: "index_fiat_transactions_on_fiat_account_id"
    t.index ["operation_type", "operation_id"], name: "index_fiat_transactions_on_operation"
    t.index ["operation_type", "operation_id"], name: "index_fiat_transactions_on_operation_type_and_operation_id"
    t.index ["reference"], name: "index_fiat_transactions_on_reference"
    t.index ["status"], name: "index_fiat_transactions_on_status"
  end

  create_table "fiat_withdrawals", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.bigint "fiat_account_id", null: false
    t.string "currency", null: false
    t.string "country_code", null: false
    t.decimal "fiat_amount", precision: 32, scale: 16, null: false
    t.decimal "fee", precision: 32, scale: 16, default: "0.0"
    t.decimal "amount_after_transfer_fee", precision: 32, scale: 16
    t.string "bank_name", null: false
    t.string "bank_account_name", null: false
    t.string "bank_account_number", null: false
    t.string "bank_branch"
    t.string "status", default: "pending", null: false
    t.integer "retry_count", default: 0
    t.text "error_message"
    t.string "cancel_reason"
    t.datetime "processed_at"
    t.datetime "cancelled_at"
    t.string "withdrawable_type"
    t.bigint "withdrawable_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "bank_reference"
    t.datetime "bank_transaction_date"
    t.jsonb "bank_response_data", default: {}
    t.string "verification_status"
    t.integer "verification_attempts", default: 0
    t.index ["bank_reference"], name: "index_fiat_withdrawals_on_bank_reference"
    t.index ["country_code"], name: "index_fiat_withdrawals_on_country_code"
    t.index ["fiat_account_id"], name: "index_fiat_withdrawals_on_fiat_account_id"
    t.index ["status", "created_at"], name: "index_fiat_withdrawals_on_status_and_created_at"
    t.index ["status"], name: "index_fiat_withdrawals_on_status"
    t.index ["user_id", "created_at"], name: "index_fiat_withdrawals_on_user_id_and_created_at"
    t.index ["user_id", "status"], name: "index_fiat_withdrawals_on_user_id_and_status"
    t.index ["user_id"], name: "index_fiat_withdrawals_on_user_id"
    t.index ["withdrawable_type", "withdrawable_id"], name: "idx_on_withdrawable_type_withdrawable_id_a5955e33f7"
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
    t.decimal "exchange_rate", precision: 20, scale: 8
    t.index ["created_at"], name: "index_merchant_escrows_on_created_at"
    t.index ["fiat_account_id"], name: "index_merchant_escrows_on_fiat_account_id"
    t.index ["fiat_currency"], name: "index_merchant_escrows_on_fiat_currency"
    t.index ["status"], name: "index_merchant_escrows_on_status"
    t.index ["usdt_account_id"], name: "index_merchant_escrows_on_usdt_account_id"
    t.index ["user_id"], name: "index_merchant_escrows_on_user_id"
  end

  create_table "messages", force: :cascade do |t|
    t.bigint "trade_id", null: false
    t.bigint "user_id", null: false
    t.text "body", null: false
    t.boolean "is_receipt_proof", default: false
    t.boolean "is_system", default: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["is_receipt_proof"], name: "index_messages_on_is_receipt_proof"
    t.index ["is_system"], name: "index_messages_on_is_system"
    t.index ["trade_id"], name: "index_messages_on_trade_id"
    t.index ["user_id"], name: "index_messages_on_user_id"
  end

  create_table "notifications", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "title", null: false
    t.text "content", null: false
    t.string "notification_type", null: false
    t.boolean "read", default: false
    t.boolean "delivered", default: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id", "created_at"], name: "index_notifications_on_user_id_and_created_at"
    t.index ["user_id"], name: "index_notifications_on_user_id"
  end

  create_table "offers", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "offer_type", null: false
    t.string "coin_currency", null: false
    t.string "currency", null: false
    t.decimal "price", precision: 32, scale: 16, null: false
    t.decimal "min_amount", precision: 32, scale: 16, null: false
    t.decimal "max_amount", precision: 32, scale: 16, null: false
    t.decimal "total_amount", precision: 32, scale: 16, null: false
    t.bigint "payment_method_id"
    t.integer "payment_time", default: 30, null: false
    t.jsonb "payment_details", default: {}
    t.string "country_code", null: false
    t.boolean "disabled", default: false
    t.boolean "deleted", default: false
    t.boolean "automatic", default: false
    t.boolean "online", default: true
    t.text "terms_of_trade"
    t.string "disable_reason"
    t.decimal "margin", precision: 10, scale: 4
    t.decimal "fixed_coin_price", precision: 32, scale: 16
    t.string "bank_names", default: [], array: true
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "schedule_start_time"
    t.datetime "schedule_end_time"
    t.index ["coin_currency", "currency", "offer_type", "disabled", "deleted"], name: "idx_on_coin_currency_currency_offer_type_disabled_d_97f0c3ccae"
    t.index ["country_code"], name: "index_offers_on_country_code"
    t.index ["payment_method_id"], name: "index_offers_on_payment_method_id"
    t.index ["schedule_end_time"], name: "index_offers_on_schedule_end_time"
    t.index ["schedule_start_time"], name: "index_offers_on_schedule_start_time"
    t.index ["user_id", "offer_type"], name: "index_offers_on_user_id_and_offer_type"
    t.index ["user_id"], name: "index_offers_on_user_id"
  end

  create_table "payment_methods", force: :cascade do |t|
    t.string "name", null: false
    t.string "display_name", null: false
    t.text "description"
    t.string "country_code", null: false
    t.boolean "enabled", default: true
    t.jsonb "fields_required", default: {}
    t.string "icon_url"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["country_code"], name: "index_payment_methods_on_country_code"
    t.index ["name"], name: "index_payment_methods_on_name", unique: true
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

  create_table "ticks", force: :cascade do |t|
    t.string "pool_pair", null: false
    t.integer "tick_index", null: false
    t.decimal "liquidity_gross", precision: 36, scale: 18, default: "0.0"
    t.decimal "liquidity_net", precision: 36, scale: 18, default: "0.0"
    t.decimal "fee_growth_outside0", precision: 36, scale: 18, default: "0.0"
    t.decimal "fee_growth_outside1", precision: 36, scale: 18, default: "0.0"
    t.bigint "tick_initialized_timestamp"
    t.boolean "initialized", default: false
    t.string "status", default: "inactive"
    t.string "tick_key", null: false
    t.bigint "amm_pool_id"
    t.bigint "created_at_timestamp"
    t.bigint "updated_at_timestamp"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["amm_pool_id"], name: "index_ticks_on_amm_pool_id"
    t.index ["pool_pair", "tick_index"], name: "index_ticks_on_pool_pair_and_tick_index", unique: true
    t.index ["pool_pair"], name: "index_ticks_on_pool_pair"
    t.index ["status"], name: "index_ticks_on_status"
    t.index ["tick_index"], name: "index_ticks_on_tick_index"
    t.index ["tick_key"], name: "index_ticks_on_tick_key", unique: true
  end

  create_table "trades", force: :cascade do |t|
    t.string "ref", null: false
    t.bigint "buyer_id", null: false
    t.bigint "seller_id", null: false
    t.bigint "offer_id", null: false
    t.string "coin_currency", null: false
    t.string "fiat_currency", null: false
    t.decimal "coin_amount", precision: 32, scale: 16, null: false
    t.decimal "fiat_amount", precision: 32, scale: 16, null: false
    t.decimal "price", precision: 32, scale: 16, null: false
    t.decimal "fee_ratio", precision: 10, scale: 4, null: false
    t.decimal "coin_trading_fee", precision: 32, scale: 16, null: false
    t.string "payment_method", null: false
    t.jsonb "payment_details", default: {}
    t.string "taker_side", null: false
    t.string "status", default: "awaiting", null: false
    t.datetime "paid_at"
    t.datetime "released_at"
    t.datetime "expired_at"
    t.datetime "cancelled_at"
    t.datetime "disputed_at"
    t.jsonb "payment_receipt_details", default: {}
    t.boolean "has_payment_proof", default: false
    t.string "payment_proof_status"
    t.text "dispute_reason"
    t.decimal "open_coin_price", precision: 32, scale: 16
    t.decimal "close_coin_price", precision: 32, scale: 16
    t.decimal "release_coin_price", precision: 32, scale: 16
    t.bigint "fiat_token_deposit_id"
    t.bigint "fiat_token_withdrawal_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.decimal "amount"
    t.decimal "token_amount"
    t.index ["buyer_id", "status"], name: "index_trades_on_buyer_id_and_status"
    t.index ["buyer_id"], name: "index_trades_on_buyer_id"
    t.index ["fiat_token_deposit_id"], name: "index_trades_on_fiat_token_deposit_id"
    t.index ["fiat_token_withdrawal_id"], name: "index_trades_on_fiat_token_withdrawal_id"
    t.index ["offer_id"], name: "index_trades_on_offer_id"
    t.index ["ref"], name: "index_trades_on_ref", unique: true
    t.index ["seller_id", "status"], name: "index_trades_on_seller_id_and_status"
    t.index ["seller_id"], name: "index_trades_on_seller_id"
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

  add_foreign_key "amm_orders", "amm_pools"
  add_foreign_key "amm_orders", "users"
  add_foreign_key "amm_positions", "amm_pools"
  add_foreign_key "amm_positions", "users"
  add_foreign_key "bank_accounts", "users"
  add_foreign_key "coin_accounts", "users"
  add_foreign_key "coin_deposit_operations", "coin_accounts"
  add_foreign_key "coin_deposit_operations", "coin_deposits"
  add_foreign_key "coin_deposits", "coin_accounts"
  add_foreign_key "coin_deposits", "users"
  add_foreign_key "coin_internal_transfer_operations", "coin_withdrawals"
  add_foreign_key "coin_internal_transfer_operations", "users", column: "receiver_id"
  add_foreign_key "coin_internal_transfer_operations", "users", column: "sender_id"
  add_foreign_key "coin_transactions", "coin_accounts"
  add_foreign_key "fiat_accounts", "users"
  add_foreign_key "fiat_deposits", "fiat_accounts"
  add_foreign_key "fiat_deposits", "users"
  add_foreign_key "fiat_transactions", "fiat_accounts"
  add_foreign_key "fiat_withdrawals", "fiat_accounts"
  add_foreign_key "fiat_withdrawals", "users"
  add_foreign_key "merchant_escrow_operations", "coin_accounts", column: "usdt_account_id"
  add_foreign_key "merchant_escrow_operations", "fiat_accounts"
  add_foreign_key "merchant_escrow_operations", "merchant_escrows"
  add_foreign_key "merchant_escrows", "coin_accounts", column: "usdt_account_id"
  add_foreign_key "merchant_escrows", "fiat_accounts"
  add_foreign_key "merchant_escrows", "users"
  add_foreign_key "messages", "trades"
  add_foreign_key "messages", "users"
  add_foreign_key "notifications", "users"
  add_foreign_key "offers", "payment_methods"
  add_foreign_key "offers", "users"
  add_foreign_key "social_accounts", "users"
  add_foreign_key "ticks", "amm_pools"
  add_foreign_key "trades", "fiat_deposits", column: "fiat_token_deposit_id"
  add_foreign_key "trades", "fiat_withdrawals", column: "fiat_token_withdrawal_id"
  add_foreign_key "trades", "offers"
  add_foreign_key "trades", "users", column: "buyer_id"
  add_foreign_key "trades", "users", column: "seller_id"
end
