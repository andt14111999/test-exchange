# frozen_string_literal: true

class AddCountryCodeToFiatAccounts < ActiveRecord::Migration[8.0]
  disable_ddl_transaction!

  def change
    add_column :fiat_accounts, :country_code, :string
    add_column :fiat_accounts, :frozen_reason, :string

    add_index :fiat_accounts, :country_code, algorithm: :concurrently
  end
end
