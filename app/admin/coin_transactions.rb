# frozen_string_literal: true

ActiveAdmin.register CoinTransaction do
  menu priority: 6, parent: 'Coin Management', label: 'Transactions'

  actions :index, :show

  filter :coin_account
  filter :coin_currency
  filter :amount
  filter :operation_type
  filter :created_at

  index do
    selectable_column
    id_column
    column :coin_account
    column :coin_currency
    column :amount do |tx|
      number_with_precision(tx.amount, precision: 8)
    end
    column :snapshot_balance do |tx|
      number_with_precision(tx.snapshot_balance, precision: 8)
    end
    column :snapshot_frozen_balance do |tx|
      number_with_precision(tx.snapshot_frozen_balance, precision: 8)
    end
    column :operation_type
    column :operation do |tx|
      link_to "#{tx.operation_type} ##{tx.operation_id}",
        polymorphic_path([ :admin, tx.operation ])
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :coin_account
      row :coin_currency
      row :amount do |tx|
        number_with_precision(tx.amount, precision: 8)
      end
      row :snapshot_balance do |tx|
        number_with_precision(tx.snapshot_balance, precision: 8)
      end
      row :snapshot_frozen_balance do |tx|
        number_with_precision(tx.snapshot_frozen_balance, precision: 8)
      end
      row :snapshot_payment_quota do |tx|
        code tx.snapshot_payment_quota
      end
      row :operation_type
      row :operation do |tx|
        link_to "#{tx.operation_type} ##{tx.operation_id}",
          polymorphic_path([ :admin, tx.operation ])
      end
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
