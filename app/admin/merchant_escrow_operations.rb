# frozen_string_literal: true

ActiveAdmin.register MerchantEscrowOperation do
  menu priority: 2, parent: 'Merchant Management', label: 'Escrow Operations'

  actions :index, :show

  filter :id
  filter :merchant_escrow
  filter :usdt_account
  filter :fiat_account
  filter :usdt_amount
  filter :fiat_amount
  filter :fiat_currency, as: :select, collection: FiatAccount::SUPPORTED_CURRENCIES.keys
  filter :operation_type, as: :select, collection: %w[freeze unfreeze]
  filter :status, as: :select, collection: -> { MerchantEscrowOperation.aasm.states.map(&:name) }
  filter :created_at

  index do
    id_column
    column :merchant_escrow
    column :operation_type
    column :usdt_amount do |op|
      number_with_precision(op.usdt_amount, precision: 8)
    end
    column :fiat_amount do |op|
      number_with_precision(op.fiat_amount, precision: 2)
    end
    column :fiat_currency
    column :status do |op|
      status_tag op.status
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :merchant_escrow
      row :usdt_account
      row :fiat_account
      row :operation_type
      row :usdt_amount do |op|
        number_with_precision(op.usdt_amount, precision: 8)
      end
      row :fiat_amount do |op|
        number_with_precision(op.fiat_amount, precision: 2)
      end
      row :fiat_currency
      row :status do |op|
        status_tag op.status
      end
      row :status_explanation
      row :created_at
      row :updated_at
    end

    panel 'Coin Transactions' do
      table_for merchant_escrow_operation.coin_transactions do
        column :id
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
      end
    end

    panel 'Fiat Transactions' do
      table_for merchant_escrow_operation.fiat_transactions do
        column :id
        column :amount do |tx|
          number_with_precision(tx.amount, precision: 2)
        end
        column :snapshot_balance do |tx|
          number_with_precision(tx.snapshot_balance, precision: 2)
        end
        column :snapshot_frozen_balance do |tx|
          number_with_precision(tx.snapshot_frozen_balance, precision: 2)
        end
        column :created_at
      end
    end

    active_admin_comments
  end
end
