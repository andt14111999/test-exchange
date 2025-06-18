# frozen_string_literal: true

ActiveAdmin.register CoinWithdrawal do
  menu priority: 2, parent: 'Coin Management', label: 'Withdrawals'

  actions :index, :show

  filter :id
  filter :user
  filter :coin_account
  filter :coin_currency
  filter :coin_amount
  filter :coin_fee
  filter :coin_address
  filter :receiver_username
  filter :coin_layer
  filter :status, as: :select, collection: -> { CoinWithdrawal.aasm.states.map(&:name) }
  filter :created_at
  filter :updated_at

  index do
    id_column
    column :user
    column :coin_account
    tag_column :coin_currency
    column :coin_amount
    column :coin_fee
    column :coin_address
    column :receiver_username
    tag_column :coin_layer
    column :status do |withdrawal|
      status_tag withdrawal.status
    end
    column :created_at
    column :updated_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :coin_account
      tag_row :coin_currency
      row :coin_amount
      row :coin_fee
      row :coin_address
      row :tx_hash
      row :receiver_username
      row :receiver_email
      row :receiver_phone_number
      tag_row :coin_layer
      row :status do |withdrawal|
        status_tag withdrawal.status
      end
      row :created_at
      row :updated_at
    end

    panel 'Withdrawal Operation', id: 'withdrawal-operation' do
      if resource.coin_withdrawal_operation.present?
        attributes_table_for resource.coin_withdrawal_operation do
          row :id
          row :status do |operation|
            status_tag operation.status
          end
          tag_row :withdrawal_status
          row :tx_hash
          row :scheduled_at
          row :withdrawal_data
          row :created_at
          row :updated_at
        end
      else
        para 'No withdrawal operation found'
      end
    end

    panel 'Transaction', id: 'transaction' do
      if resource.coin_withdrawal_operation.present? && resource.coin_withdrawal_operation.coin_transactions.exists?
        attributes_table_for resource.coin_withdrawal_operation.coin_transactions.first do
          row :id
          row :amount
          tag_row :coin_currency
          tag_row :transaction_type
          row :created_at
          row :updated_at
        end
      else
        para 'No transaction found'
      end
    end

    active_admin_comments
  end

  sidebar 'State Actions', only: :show do
    if resource.pending?
      button_to 'Cancel Withdrawal',
        cancel_admin_coin_withdrawal_path(resource),
        method: :put,
        data: { confirm: 'Are you sure?' }
    end
  end

  member_action :cancel, method: :put do
    withdrawal = resource
    begin
      withdrawal.cancel!
      redirect_to resource_path, notice: 'Withdrawal was successfully cancelled'
    rescue StandardError => e
      redirect_to resource_path, alert: 'Could not cancel withdrawal'
    end
  end
end
