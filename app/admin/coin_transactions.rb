# frozen_string_literal: true

ActiveAdmin.register CoinTransaction do
  menu priority: 7, parent: 'Coin Management', label: 'Transactions'

  permit_params :user_id, :coin_type, :amount, :fee, :status,
    :reference_type, :reference_id

  index do
    selectable_column
    id_column
    column :user
    column :coin_type
    column :amount do |transaction|
      number_with_precision(transaction.amount, precision: 8)
    end
    column :fee do |transaction|
      number_with_precision(transaction.fee, precision: 8)
    end
    column :status do |transaction|
      status_tag transaction.status
    end
    column :reference_type
    column :reference_id
    column :created_at
    actions
  end

  filter :user
  filter :coin_type
  filter :amount
  filter :status
  filter :reference_type
  filter :reference_id
  filter :created_at
  filter :updated_at

  show do
    attributes_table do
      row :id
      row :user
      row :coin_type
      row :amount do |transaction|
        number_with_precision(transaction.amount, precision: 8)
      end
      row :fee do |transaction|
        number_with_precision(transaction.fee, precision: 8)
      end
            row :status do |transaction|
        status_tag transaction.status
            end
      row :reference_type
      row :reference_id
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs do
      f.input :user
      f.input :coin_type
      f.input :amount
      f.input :fee
      f.input :status
      f.input :reference_type, as: :select, collection: CoinTransaction::REFERENCE_TYPES
      f.input :reference_id
    end
    f.actions
  end
end
