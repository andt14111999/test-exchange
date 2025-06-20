# frozen_string_literal: true

ActiveAdmin.register CoinInternalTransferOperation do
  menu priority: 4, parent: 'Coin Management', label: 'Internal Transfer Operations'

  actions :index, :show

  filter :id
  filter :coin_withdrawal
  filter :sender, as: :select, collection: proc { User.pluck(:email, :id) }
  filter :receiver, as: :select, collection: proc { User.pluck(:email, :id) }
  filter :coin_currency
  filter :coin_amount
  filter :coin_fee
  filter :status, as: :select, collection: -> { CoinInternalTransferOperation.aasm.states.map(&:name) }
  filter :created_at
  filter :updated_at

  index do
    id_column
    column :coin_withdrawal
    column :sender do |operation|
      link_to operation.sender.email, admin_user_path(operation.sender)
    end
    column :receiver do |operation|
      link_to operation.receiver.email, admin_user_path(operation.receiver)
    end
    tag_column :coin_currency
    column :coin_amount
    column :coin_fee
    column :status do |operation|
      status_tag operation.status
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :coin_withdrawal
      row :sender do |operation|
        link_to operation.sender.email, admin_user_path(operation.sender)
      end
      row :receiver do |operation|
        link_to operation.receiver.email, admin_user_path(operation.receiver)
      end
      tag_row :coin_currency
      row :coin_amount
      row :coin_fee
      row :status do |operation|
        status_tag operation.status
      end
      row :status_explanation
      row :created_at
      row :updated_at
    end

    div id: 'transactions' do
      panel 'Transactions' do
        table_for resource.coin_transactions do
          column :id
          column :amount
          column :coin_currency
          column :coin_account do |transaction|
            link_to transaction.coin_account.user.email, admin_user_path(transaction.coin_account.user)
          end
          column :created_at
          column :updated_at
        end
      end
    end

    active_admin_comments
  end

  sidebar 'State Actions', only: :show do
    div id: 'state_actions' do
      if resource.may_process?
        button_to 'Process Transfer',
          process_transfer_admin_coin_internal_transfer_operation_path(resource),
          method: :put,
          data: { confirm: 'Are you sure?' }
      end

      if resource.may_reject?
        button_to 'Reject Transfer',
          reject_admin_coin_internal_transfer_operation_path(resource),
          method: :put,
          data: { confirm: 'Are you sure?' }
      end

      if resource.may_cancel?
        button_to 'Cancel Transfer',
          cancel_admin_coin_internal_transfer_operation_path(resource),
          method: :put,
          data: { confirm: 'Are you sure?' }
      end
    end
  end

  member_action :process_transfer, method: :put do
    operation = resource
    if operation.may_process?
      operation.process!
      redirect_to resource_path, notice: 'Internal transfer operation processing started'
    else
      redirect_to resource_path, alert: 'Cannot process this operation'
    end
  end

  member_action :reject, method: :put do
    operation = resource
    if operation.may_reject?
      operation.reject!
      redirect_to resource_path, notice: 'Internal transfer operation rejected'
    else
      redirect_to resource_path, alert: 'Cannot reject this operation'
    end
  end

  member_action :cancel, method: :put do
    operation = resource
    if operation.may_cancel?
      operation.cancel!
      redirect_to resource_path, notice: 'Internal transfer operation cancelled'
    else
      redirect_to resource_path, alert: 'Cannot cancel this operation'
    end
  end
end
