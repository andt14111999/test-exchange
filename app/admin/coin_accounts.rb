# frozen_string_literal: true

ActiveAdmin.register CoinAccount do
  menu priority: 1, parent: 'Coin Management', label: 'Accounts'

  actions :all, except: [ :edit, :update, :new ]

  permit_params :user_id, :coin_currency, :layer, :balance, :frozen_balance, :address, :account_type

  index do
    selectable_column
    id_column
    column :user
    column :coin_currency
    column :layer
    column :account_type
    column :balance do |account|
      number_with_precision(account.balance, precision: 8)
    end
    column :frozen_balance do |account|
      number_with_precision(account.frozen_balance, precision: 8)
    end
    column :address
    column :created_at
    actions
  end

  filter :user
  filter :coin_currency, as: :select, collection: CoinAccount::SUPPORTED_NETWORKS.keys
  filter :layer
  filter :account_type, as: :select, collection: CoinAccount::ACCOUNT_TYPES
  filter :balance
  filter :frozen_balance
  filter :address
  filter :created_at

  show do
    attributes_table do
      row :id
      row :user
      row :coin_currency
      row :layer
      row :account_type
      row :balance do |account|
        number_with_precision(account.balance, precision: 8)
      end
      row :frozen_balance do |account|
        number_with_precision(account.frozen_balance, precision: 8)
      end
      row :address
      row :created_at
      row :updated_at
    end

    panel 'Total Balances Across All Layers' do
      main_account = coin_account.user.coin_accounts.of_coin(coin_account.coin_currency).main
      attributes_table_for main_account do
        row :frozen_balance do
          number_with_precision(main_account.frozen_balance, precision: 8)
        end
        row :available_balance do
          number_with_precision(main_account.available_balance, precision: 8)
        end
      end
    end
  end
end
