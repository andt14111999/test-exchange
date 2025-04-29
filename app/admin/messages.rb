# frozen_string_literal: true

ActiveAdmin.register Message do
  permit_params :trade_id, :user_id, :body, :is_receipt_proof, :is_system

  scope :all
  scope :receipt_proofs
  scope :system_messages
  scope :user_messages
  scope :sorted

  filter :id
  filter :trade
  filter :user
  filter :body
  filter :is_receipt_proof
  filter :is_system
  filter :created_at
  filter :updated_at

  index do
    selectable_column
    id_column
    column :trade do |message|
      link_to "Trade ##{message.trade.ref}", admin_trade_path(message.trade)
    end
    column :user
    column :body
    column :is_receipt_proof
    column :is_system
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :trade do |message|
        link_to "Trade ##{message.trade.ref}", admin_trade_path(message.trade)
      end
      row :user
      row :body
      row :is_receipt_proof
      row :is_system
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs 'Message Details' do
      f.input :trade
      f.input :user
      f.input :body
      f.input :is_receipt_proof
      f.input :is_system
    end
    f.actions
  end

  # Custom actions
  action_item :mark_as_receipt_proof, only: :show, if: proc { !resource.is_receipt_proof? } do
    link_to 'Mark as Receipt Proof', mark_as_receipt_proof_admin_message_path(resource), method: :put
  end

  action_item :mark_as_regular_message, only: :show, if: proc { resource.is_receipt_proof? } do
    link_to 'Mark as Regular Message', mark_as_regular_message_admin_message_path(resource), method: :put
  end

  member_action :mark_as_receipt_proof, method: :put do
    resource.mark_as_receipt_proof!
    redirect_to admin_message_path(resource), notice: 'Message has been marked as a receipt proof'
  end

  member_action :mark_as_regular_message, method: :put do
    resource.mark_as_regular_message!
    redirect_to admin_message_path(resource), notice: 'Message has been marked as a regular message'
  end
end
