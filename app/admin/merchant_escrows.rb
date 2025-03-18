# frozen_string_literal: true

ActiveAdmin.register MerchantEscrow do
  menu priority: 1, parent: 'Merchant Management', label: 'Escrows'

  actions :index, :show

  filter :id
  filter :user
  filter :usdt_account
  filter :fiat_account
  filter :usdt_amount
  filter :fiat_amount
  filter :fiat_currency, as: :select, collection: FiatAccount::SUPPORTED_CURRENCIES.keys
  filter :status, as: :select, collection: -> { MerchantEscrow.aasm.states.map(&:name) }
  filter :created_at
  filter :completed_at

  index do
    id_column
    column :user
    column :usdt_amount do |escrow|
      number_with_precision(escrow.usdt_amount, precision: 8)
    end
    column :fiat_amount do |escrow|
      number_with_precision(escrow.fiat_amount, precision: 2)
    end
    column :fiat_currency
    column :status do |escrow|
      status_tag escrow.status
    end
    column :completed_at
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :usdt_account
      row :fiat_account
      row :usdt_amount do |escrow|
        number_with_precision(escrow.usdt_amount, precision: 8)
      end
      row :fiat_amount do |escrow|
        number_with_precision(escrow.fiat_amount, precision: 2)
      end
      row :fiat_currency
      row :status do |escrow|
        status_tag escrow.status
      end
      row :completed_at
      row :created_at
      row :updated_at
    end

    panel 'Escrow Operations' do
      table_for merchant_escrow.merchant_escrow_operations do
        column :id
        column :operation_type
        column :usdt_amount do |op|
          number_with_precision(op.usdt_amount, precision: 8)
        end
        column :fiat_amount do |op|
          number_with_precision(op.fiat_amount, precision: 2)
        end
        column :status do |op|
          status_tag op.status
        end
        column :created_at
        column :actions do |op|
          link_to 'View', admin_merchant_escrow_operation_path(op)
        end
      end
    end

    active_admin_comments
  end

  sidebar 'State Actions', only: :show do
    if resource.pending?
      button_to 'Cancel Escrow',
        cancel_admin_merchant_escrow_path(resource),
        method: :put,
        data: { confirm: 'Are you sure?' }
    end
  end

  member_action :cancel, method: :put do
    escrow = resource
    if escrow.cancel
      redirect_to resource_path, notice: 'Escrow was successfully cancelled'
    else
      redirect_to resource_path, alert: 'Could not cancel escrow'
    end
  end
end
