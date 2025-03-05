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

ActiveRecord::Schema[8.0].define(version: 2025_03_05_132741) do
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
    t.string "coin_type", null: false
    t.string "layer", null: false
    t.decimal "balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "frozen_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "total_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "available_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.string "address"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id", "coin_type", "layer"], name: "index_coin_accounts_on_user_id_and_coin_type_and_layer", unique: true
    t.index ["user_id"], name: "index_coin_accounts_on_user_id"
  end

  create_table "fiat_accounts", force: :cascade do |t|
    t.bigint "user_id", null: false
    t.string "currency", null: false
    t.decimal "balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "frozen_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "total_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.decimal "available_balance", precision: 32, scale: 16, default: "0.0", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id", "currency"], name: "index_fiat_accounts_on_user_id_and_currency", unique: true
    t.index ["user_id"], name: "index_fiat_accounts_on_user_id"
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
  add_foreign_key "fiat_accounts", "users"
  add_foreign_key "social_accounts", "users"
end
