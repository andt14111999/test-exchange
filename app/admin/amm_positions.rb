# frozen_string_literal: true

ActiveAdmin.register AmmPosition do
  menu priority: 3, label: 'AMM Positions', parent: 'AMM'
  actions :index, :show

  filter :identifier
  filter :amm_pool
  filter :status, as: :select, collection: -> { AmmPosition.aasm.states_for_select }
  filter :created_at
  filter :updated_at

  scope :all, default: true
  scope :pending
  scope :open
  scope :closed
  scope :error

  index do
    selectable_column
    id_column
    column :identifier
    column :user
    column :pool_pair
    column :status do |position|
      status_tag position.status
    end
    column :liquidity do |position|
      number_with_delimiter(position.liquidity.to_f.round(6))
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
      row :pool_pair
      row :account_key0
      row :account_key1
      row :status do |position|
        status_tag position.status
      end
      row :error_message
      row :tick_lower_index
      row :tick_upper_index
      row :liquidity do |position|
        number_with_delimiter(position.liquidity.to_f.round(6))
      end
      row :slippage do |position|
        "#{(position.slippage.to_f * 100).round(2)}%"
      end
      row :amount0 do |position|
        number_with_delimiter(position.amount0.to_f.round(6))
      end
      row :amount1 do |position|
        number_with_delimiter(position.amount1.to_f.round(6))
      end
      row :amount0_initial do |position|
        number_with_delimiter(position.amount0_initial.to_f.round(6))
      end
      row :amount1_initial do |position|
        number_with_delimiter(position.amount1_initial.to_f.round(6))
      end
      row :fee_growth_inside0_last do |position|
        number_with_delimiter(position.fee_growth_inside0_last.to_f.round(6))
      end
      row :fee_growth_inside1_last do |position|
        number_with_delimiter(position.fee_growth_inside1_last.to_f.round(6))
      end
      row :tokens_owed0 do |position|
        number_with_delimiter(position.tokens_owed0.to_f.round(6))
      end
      row :tokens_owed1 do |position|
        number_with_delimiter(position.tokens_owed1.to_f.round(6))
      end
      row :fee_collected0 do |position|
        number_with_delimiter(position.fee_collected0.to_f.round(6))
      end
      row :fee_collected1 do |position|
        number_with_delimiter(position.fee_collected1.to_f.round(6))
      end
      row :amount0_withdrawal do |position|
        number_with_delimiter(position.amount0_withdrawal.to_f.round(6))
      end
      row :amount1_withdrawal do |position|
        number_with_delimiter(position.amount1_withdrawal.to_f.round(6))
      end
      row :estimate_fee_token0 do |position|
        number_with_delimiter(position.estimate_fee_token0.to_f.round(6))
      end
      row :estimate_fee_token1 do |position|
        number_with_delimiter(position.estimate_fee_token1.to_f.round(6))
      end
      row :apr do |position|
        "#{position.apr.to_f.round(2)}%"
      end
      row :created_at
      row :updated_at
    end
  end

  controller do
    def scoped_collection
      super.includes(:user, :amm_pool)
    end
  end
end
