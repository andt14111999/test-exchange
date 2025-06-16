# frozen_string_literal: true

ActiveAdmin.register CoinTransaction do
  menu priority: 6, parent: 'Coin Management', label: 'Transactions'

  actions :index, :show

  filter :coin_account
  filter :coin_currency, as: :select, collection: CoinAccount::SUPPORTED_NETWORKS.keys
  filter :transaction_type, as: :select, collection: %w[transfer lock unlock]
  filter :amount
  filter :created_at

  index do
    selectable_column
    id_column
    column :coin_account do |tx|
      link_to "Coin account ##{tx.coin_account.id}", admin_coin_account_path(tx.coin_account)
    end
    column :user do |tx|
      link_to tx.coin_account.user.email, admin_user_path(tx.coin_account.user)
    end
    tag_column :coin_currency
    tag_column :transaction_type
    column :amount do |tx|
      number_with_precision(tx.amount, precision: 8)
    end
    column :snapshot_balance do |tx|
      number_with_precision(tx.snapshot_balance, precision: 8)
    end
    column :snapshot_frozen_balance do |tx|
      number_with_precision(tx.snapshot_frozen_balance, precision: 8)
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :coin_account do |tx|
        link_to "Coin account ##{tx.coin_account.id}", admin_coin_account_path(tx.coin_account)
      end
      row :user do |tx|
        link_to tx.coin_account.user.email, admin_user_path(tx.coin_account.user)
      end
      tag_row :coin_currency
      tag_row :transaction_type
      row :amount do |tx|
        number_with_precision(tx.amount, precision: 8)
      end
      row :snapshot_balance do |tx|
        number_with_precision(tx.snapshot_balance, precision: 8)
      end
      row :snapshot_frozen_balance do |tx|
        number_with_precision(tx.snapshot_frozen_balance, precision: 8)
      end
      row :error_message
      row :created_at
      row :updated_at
    end
  end

  sidebar 'Balance Changes', only: :show do
    attributes_table do
      row 'Previous Balance' do |tx|
        number_with_precision(tx.snapshot_balance, precision: 8)
      end
      row 'Change' do |tx|
        status_tag(tx.amount.positive? ? 'increase' : 'decrease',
          class: tx.amount.positive? ? 'green' : 'red')
        number_with_precision(tx.amount.abs, precision: 8)
      end
      row 'New Balance' do |tx|
        number_with_precision(tx.snapshot_balance + tx.amount, precision: 8)
      end
    end
  end
end
