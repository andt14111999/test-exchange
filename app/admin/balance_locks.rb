# frozen_string_literal: true

ActiveAdmin.register BalanceLock do
  menu priority: 7, parent: 'Coin Management', label: 'Balance Locks'

  actions :all, except: [ :new, :edit, :destroy ]

  index do
    selectable_column
    id_column
    column :user do |balance_lock|
      link_to balance_lock.user.email, admin_user_path(balance_lock.user)
    end
    column :locked_balances do |balance_lock|
      balance_lock.locked_balances.map { |coin, amount| "#{coin}: #{amount}" }.join(', ')
    end
    tag_column :status
    column :performer
    column :reason
    column :locked_at
    column :unlocked_at
    column :created_at
    actions do |balance_lock|
      if balance_lock.locked?
        link_to 'Release', release_admin_balance_lock_path(balance_lock),
                method: :patch,
                data: { confirm: 'Are you sure you want to release this balance?' },
                class: 'button'
      end
    end
  end

  filter :user
  filter :status, as: :select, collection: %w[locked released]
  filter :locked_at
  filter :unlocked_at
  filter :created_at

  show do
    attributes_table do
      row :id
      row :user do |balance_lock|
        link_to balance_lock.user.email, admin_user_path(balance_lock.user)
      end
      row :locked_balances do |balance_lock|
        table_for balance_lock.locked_balances do
          column 'Coin' do |coin, _amount|
            coin
          end
          column 'Amount' do |_coin, amount|
            amount
          end
        end
      end
      tag_row :status
      row :engine_lock_id
      row :performer do |balance_lock|
        if balance_lock.performer.present?
          balance_lock.performer
        else
          status_tag 'Unknown', class: 'warning'
        end
      end
      row :reason
      row :error_message
      row :locked_at
      row :unlocked_at
      row :created_at
      row :updated_at
    end

    panel 'Balance Lock Operations' do
      table_for balance_lock.balance_lock_operations do
        column :id do |operation|
          link_to operation.id, admin_balance_lock_operation_path(operation)
        end
        tag_column :operation_type
        tag_column :status
        column :status_explanation
        column :created_at
      end
    end
  end

  action_item :release, only: :show do
    if resource.locked?
      link_to 'Release',
        release_admin_balance_lock_path(resource),
        method: :patch,
        data: { confirm: 'Are you sure you want to release this balance?' }
    end
  end

  member_action :release, method: :patch do
    resource.start_releasing!
    redirect_to admin_balance_lock_path(resource), notice: 'Balance lock has been released successfully.'
  rescue StandardError => e
    redirect_to admin_balance_lock_path(resource), alert: "Failed to release balance: #{e.message}"
  end
end
