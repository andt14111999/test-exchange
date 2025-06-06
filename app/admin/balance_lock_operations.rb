# frozen_string_literal: true

ActiveAdmin.register BalanceLockOperation do
  menu priority: 8, parent: 'Coin Management', label: 'Balance Lock Operations'

  actions :index, :show

  index do
    selectable_column
    id_column
    column :balance_lock do |operation|
      link_to "Lock ##{operation.balance_lock.id}", admin_balance_lock_path(operation.balance_lock)
    end
    column :user do |operation|
      link_to operation.user.email, admin_user_path(operation.user)
    end
    column :locked_balances do |operation|
      operation.locked_balances.map { |coin, amount| "#{coin}: #{amount}" }.join(', ')
    end
    tag_column :operation_type
    tag_column :status
    column :created_at
    actions
  end

  filter :balance_lock
  filter :operation_type, as: :select, collection: %w[lock unlock]
  filter :status, as: :select, collection: %w[pending processing completed failed]
  filter :created_at

  show do
    attributes_table do
      row :id
      row :balance_lock do |operation|
        link_to "Lock ##{operation.balance_lock.id}", admin_balance_lock_path(operation.balance_lock)
      end
      row :user do |operation|
        link_to operation.user.email, admin_user_path(operation.user)
      end
      row :locked_balances do |operation|
        table_for operation.locked_balances do
          column 'Coin' do |coin, _amount|
            coin
          end
          column 'Amount' do |_coin, amount|
            amount
          end
        end
      end
      tag_row :operation_type
      tag_row :status
      row :status_explanation
      row :created_at
      row :updated_at
    end

    panel 'Related Coin Transactions' do
      table_for balance_lock_operation.coin_transactions do
        column :id do |transaction|
          link_to transaction.id, admin_coin_transaction_path(transaction)
        end
        column :coin_currency
        column :amount
        tag_column :transaction_type
        column :created_at
      end
    end

    panel 'Related Fiat Transactions' do
      table_for balance_lock_operation.fiat_transactions do
        column :id do |transaction|
          link_to transaction.id, admin_fiat_transaction_path(transaction)
        end
        column :currency
        column :amount
        tag_column :transaction_type
        column :created_at
      end
    end
  end
end
