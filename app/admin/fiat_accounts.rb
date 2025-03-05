# frozen_string_literal: true

CurrencyTotal = Struct.new(:currency, :name, :total_balance, :frozen_balance, :available_balance)

ActiveAdmin.register FiatAccount do
  menu priority: 4

  permit_params :user_id, :currency, :balance, :frozen_balance

  index do
    selectable_column
    id_column
    column :user
    column :currency
    column :balance do |account|
      number_with_precision(account.balance, precision: 2)
    end
    column :frozen_balance do |account|
      number_with_precision(account.frozen_balance, precision: 2)
    end
    column :total_balance do |account|
      number_with_precision(account.total_balance, precision: 2)
    end
    column :available_balance do |account|
      number_with_precision(account.available_balance, precision: 2)
    end
    column :created_at
    actions
  end

  filter :user
  filter :currency, as: :select, collection: FiatAccount::SUPPORTED_CURRENCIES
  filter :balance
  filter :frozen_balance
  filter :total_balance
  filter :available_balance
  filter :created_at

  show do
    attributes_table do
      row :id
      row :user
      row :currency
      row :balance do |account|
        number_with_precision(account.balance, precision: 2)
      end
      row :frozen_balance do |account|
        number_with_precision(account.frozen_balance, precision: 2)
      end
      row :total_balance do |account|
        number_with_precision(account.total_balance, precision: 2)
      end
      row :available_balance do |account|
        number_with_precision(account.available_balance, precision: 2)
      end
      row :created_at
      row :updated_at
    end

    panel 'Currency Information' do
      attributes_table_for fiat_account do
        row :currency_name do
          FiatAccount::SUPPORTED_CURRENCIES[fiat_account.currency]
        end
      end
    end
  end

  form do |f|
    f.inputs do
      f.input :user
      f.input :currency, as: :select,
        collection: FiatAccount::SUPPORTED_CURRENCIES,
        include_blank: false
      f.input :balance
      f.input :frozen_balance
    end
    f.actions
  end
end
