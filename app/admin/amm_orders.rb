# frozen_string_literal: true

ActiveAdmin.register AmmOrder do
  menu priority: 4, label: 'AMM Orders', parent: 'AMM'
  actions :index, :show

  filter :identifier
  filter :amm_pool
  filter :status, as: :select, collection: -> { AmmOrder.aasm.states_for_select }
  filter :created_at
  filter :updated_at

  scope :all, default: true
  scope :pending
  scope :processing
  scope :success
  scope :error

  index do
    id_column
    column :identifier
    column :amm_pool
    column :user
    column :zero_for_one
    column :status do |order|
      status_tag order.status
    end
    column :amount_specified do |order|
      number_with_delimiter(order.amount_specified.to_f.round(6))
    end
    column :amount_actual do |order|
      number_with_delimiter(order.amount_actual.to_f.round(6))
    end
    column :created_at
    column :updated_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :identifier
      row :user
      row :amm_pool
      row :zero_for_one
      row :status do |order|
        status_tag order.status
      end
      row :amount_specified do |order|
        number_with_delimiter(order.amount_specified.to_f.round(6))
      end
      row :amount_estimated do |order|
        number_with_delimiter(order.amount_estimated.to_f.round(6))
      end
      row :amount_actual do |order|
        number_with_delimiter(order.amount_actual.to_f.round(6))
      end
      row :amount_received do |order|
        number_with_delimiter(order.amount_received.to_f.round(6))
      end
      row :before_tick_index
      row :after_tick_index
      row :error_message
      row :fees
      row :slippage do |order|
        "#{(order.slippage.to_f * 100).round(2)}%"
      end
      row :created_at
      row :updated_at
    end
  end
end
