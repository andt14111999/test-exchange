# frozen_string_literal: true

ActiveAdmin.register CoinDeposit do
  menu priority: 5, parent: 'Coin Management', label: 'Deposits'

  permit_params :user_id, :coin_account_id, :coin_type, :amount, :fee,
    :tx_hash, :reference_id, :confirmations, :status, :blockchain_fee

  index do
    selectable_column
    id_column
    column :user
    column :coin_type
    column :amount do |deposit|
      number_with_precision(deposit.amount, precision: 8)
    end
    column :fee do |deposit|
      number_with_precision(deposit.fee, precision: 8)
    end
    column :blockchain_fee do |deposit|
      number_with_precision(deposit.blockchain_fee, precision: 8)
    end
    column :tx_hash
    column :confirmations
    column :status do |deposit|
      status_tag deposit.status
    end
    column :created_at
    actions
  end

  filter :user
  filter :coin_type
  filter :amount
  filter :status
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
      row :amount do |deposit|
        number_with_precision(deposit.amount, precision: 8)
      end
      row :fee do |deposit|
        number_with_precision(deposit.fee, precision: 8)
      end
      row :blockchain_fee do |deposit|
        number_with_precision(deposit.blockchain_fee, precision: 8)
      end
      row :tx_hash
      row :reference_id
      row :confirmations
      row :status do |deposit|
        status_tag deposit.status
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
      f.input :tx_hash
      f.input :reference_id
      f.input :confirmations
      f.input :status
    end
    f.actions
  end
end
