# frozen_string_literal: true

ActiveAdmin.register FiatTransaction do
  menu priority: 3, parent: 'Fiat Management', label: 'Fiat Transactions'

  actions :index, :show

  filter :fiat_account
  filter :currency, as: :select, collection: FiatAccount::SUPPORTED_CURRENCIES.keys
  filter :amount
  filter :transaction_type, as: :select, collection: FiatTransaction::TRANSACTION_TYPES
  filter :created_at

  index do
    selectable_column
    id_column
    column :fiat_account
    column :currency
    column :amount do |tx|
      number_with_precision(tx.amount, precision: 2)
    end
    column :transaction_type do |tx|
      status_tag tx.transaction_type, class: tx.transaction_type == 'mint' ? 'green' : 'red'
    end
    column :snapshot_balance do |tx|
      number_with_precision(tx.snapshot_balance, precision: 2)
    end
    column :snapshot_frozen_balance do |tx|
      number_with_precision(tx.snapshot_frozen_balance, precision: 2)
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :fiat_account
      row :currency
      row :amount do |tx|
        number_with_precision(tx.amount, precision: 2)
      end
      row :transaction_type do |tx|
        status_tag tx.transaction_type, class: tx.transaction_type == 'mint' ? 'green' : 'red'
      end
      row :snapshot_balance do |tx|
        number_with_precision(tx.snapshot_balance, precision: 2)
      end
      row :snapshot_frozen_balance do |tx|
        number_with_precision(tx.snapshot_frozen_balance, precision: 2)
      end
      row :created_at
      row :updated_at
    end

    panel 'Related Information' do
      attributes_table_for resource do
        row :user do |tx|
          link_to tx.user.email, admin_user_path(tx.user)
        end
      end
    end
  end

  sidebar 'Balance Changes', only: :show do
    attributes_table do
      row 'Previous Balance' do |tx|
        number_with_precision(tx.snapshot_balance, precision: 2)
      end
      row 'Change' do |tx|
        status_tag(tx.amount.positive? ? 'increase' : 'decrease',
          class: tx.amount.positive? ? 'green' : 'red')
        number_with_precision(tx.amount.abs, precision: 2)
      end
      row 'New Balance' do |tx|
        number_with_precision(tx.snapshot_balance + tx.amount, precision: 2)
      end
    end
  end
end
