# frozen_string_literal: true

ActiveAdmin.register CoinAccount do
  menu priority: 3

  permit_params :user_id, :coin_type, :layer, :balance, :frozen_balance, :address

  index do
    selectable_column
    id_column
    column :user
    column :coin_type
    column :layer
    column :balance do |account|
      number_with_precision(account.balance, precision: 8)
    end
    column :frozen_balance do |account|
      number_with_precision(account.frozen_balance, precision: 8)
    end
    column :total_balance do |account|
      number_with_precision(account.total_balance, precision: 8)
    end
    column :available_balance do |account|
      number_with_precision(account.available_balance, precision: 8)
    end
    column :address
    column :created_at
    actions
  end

  filter :user
  filter :coin_type, as: :select, collection: CoinAccount::SUPPORTED_NETWORKS.keys
  filter :layer
  filter :balance
  filter :frozen_balance
  filter :total_balance
  filter :available_balance
  filter :address
  filter :created_at

  show do
    attributes_table do
      row :id
      row :user
      row :coin_type
      row :layer
      row :balance do |account|
        number_with_precision(account.balance, precision: 8)
      end
      row :frozen_balance do |account|
        number_with_precision(account.frozen_balance, precision: 8)
      end
      row :total_balance do |account|
        number_with_precision(account.total_balance, precision: 8)
      end
      row :available_balance do |account|
        number_with_precision(account.available_balance, precision: 8)
      end
      row :address
      row :created_at
      row :updated_at
    end

    panel 'Total Balances Across All Layers' do
      main_account = coin_account.user.coin_accounts.of_coin(coin_account.coin_type).main
      attributes_table_for main_account do
        row :total_balance do
          number_with_precision(main_account.total_balance, precision: 8)
        end
        row :frozen_balance do
          number_with_precision(main_account.frozen_balance, precision: 8)
        end
        row :available_balance do
          number_with_precision(main_account.available_balance, precision: 8)
        end
      end
    end
  end

  form do |f|
    f.inputs do
      f.input :user
      f.input :coin_type, as: :select, collection: CoinAccount::SUPPORTED_NETWORKS.keys
      f.input :layer, as: :select, collection: lambda {
        f.object.coin_type.present? ? CoinAccount::SUPPORTED_NETWORKS[f.object.coin_type] || [] : []
      }.call
      f.input :balance
      f.input :frozen_balance
      f.input :address
    end
    f.actions
  end
end
