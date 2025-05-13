# frozen_string_literal: true

ActiveAdmin.register Tick do
  menu priority: 10, label: 'Ticks', parent: 'AMM'

  actions :index, :show

  filter :amm_pool
  filter :status, as: :select, collection: -> { Tick.aasm.states_for_select }
  filter :created_at
  filter :updated_at

  index do
    selectable_column
    id_column
    column :amm_pool
    column :pool_pair
    column :tick_index
    column :tick_key
    column :liquidity_gross
    column :liquidity_net
    column :status do |tick|
      status_tag tick.status, class: tick.status == 'active' ? 'ok' : 'error'
    end
    column :created_at
    column :updated_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :amm_pool
      row :pool_pair
      row :tick_index
      row :tick_key
      row :liquidity_gross
      row :liquidity_net
      row :fee_growth_outside0
      row :fee_growth_outside1
      row :tick_initialized_timestamp
      row :initialized
      row :status do |tick|
        status_tag tick.status, class: tick.status == 'active' ? 'ok' : 'error'
      end
      row :created_at
      row :updated_at
      row :created_at_timestamp
      row :updated_at_timestamp
    end
  end
end
