# frozen_string_literal: true

ActiveAdmin.register CoinWithdrawalOperation do
  menu priority: 3, parent: 'Coin Management', label: 'Withdrawal Operations'

  actions :index, :show

  filter :id
  filter :coin_withdrawal
  filter :coin_currency
  filter :coin_amount
  filter :coin_fee
  filter :status, as: :select, collection: -> { CoinWithdrawalOperation.aasm.states.map(&:name) }
  filter :withdrawal_status
  filter :tx_hash
  filter :created_at
  filter :updated_at

  index do
    id_column
    column :coin_withdrawal
    column :coin_currency
    column :coin_amount
    column :coin_fee
    column :status do |operation|
      status_tag operation.status
    end
    column :withdrawal_status
    column :tx_hash
    column :tx_hash_arrived_at
    column :scheduled_at
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :coin_withdrawal
      row :coin_currency
      row :coin_amount
      row :coin_fee
      row :status do |operation|
        status_tag operation.status
      end
      row :withdrawal_status
      row :tx_hash
      row :tx_hash_arrived_at
      row :scheduled_at
      row :withdrawal_data
      row :created_at
      row :updated_at
    end

    panel 'Transactions' do
      table_for resource.coin_transactions do
        column :id
        column :amount
        column :coin_currency
        column :status do |transaction|
          status_tag transaction.status
        end
        column :created_at
        column :updated_at
      end
    end

    active_admin_comments
  end

  sidebar 'State Actions', only: :show do
    if resource.pending?
      button_to 'Start Relaying',
        relay_admin_coin_withdrawal_operation_path(resource),
        method: :put,
        data: { confirm: 'Are you sure?' }
    end
  end

  member_action :relay, method: :put do
    operation = resource
    operation.relay_later
    redirect_to resource_path, notice: 'Started relaying withdrawal operation'
  end
end
