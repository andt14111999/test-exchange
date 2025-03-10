# frozen_string_literal: true

ActiveAdmin.register CoinWithdrawal do
  menu priority: 6, parent: 'Coin Management', label: 'Withdrawals'

  permit_params :user_id, :coin_account_id, :coin_type, :amount, :fee,
    :blockchain_fee, :destination_address, :memo, :network,
    :tx_hash, :reference_id, :status

  index do
    selectable_column
    id_column
    column :user
    column :coin_type
    column :amount do |withdrawal|
      number_with_precision(withdrawal.amount, precision: 8)
    end
    column :fee do |withdrawal|
      number_with_precision(withdrawal.fee, precision: 8)
    end
    column :blockchain_fee do |withdrawal|
      number_with_precision(withdrawal.blockchain_fee, precision: 8)
    end
    column :destination_address
    column :network
    column :status do |withdrawal|
      status_tag withdrawal.status
    end
    column :created_at
    actions
  end

  filter :user
  filter :coin_type
  filter :amount
  filter :status
  filter :destination_address
  filter :network
  filter :tx_hash
  filter :reference_id
  filter :created_at
  filter :updated_at

  show do
    attributes_table do
      row :id
      row :user
      row :coin_account
      row :coin_type
            row :amount do |withdrawal|
        number_with_precision(withdrawal.amount, precision: 8)
            end
      row :fee do |withdrawal|
        number_with_precision(withdrawal.fee, precision: 8)
      end
      row :blockchain_fee do |withdrawal|
        number_with_precision(withdrawal.blockchain_fee, precision: 8)
      end
      row :destination_address
      row :memo
      row :network
      row :tx_hash
      row :reference_id
      row :status do |withdrawal|
        status_tag withdrawal.status
      end
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs do
      f.input :user
      f.input :coin_account
      f.input :coin_type
      f.input :amount
      f.input :fee
      f.input :blockchain_fee
      f.input :destination_address
      f.input :memo
      f.input :network
      f.input :tx_hash
      f.input :reference_id
      f.input :status
    end
    f.actions
  end
end
