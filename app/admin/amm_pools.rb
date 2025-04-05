# frozen_string_literal: true

ActiveAdmin.register AmmPool do
  menu priority: 10, label: 'AMM Pools'

  actions :all

  scope :all
  scope :active
  scope :inactive
  scope :failed

  permit_params :pair, :token0, :token1, :tick_spacing, :fee_percentage, :fee_protocol_percentage, :status

  controller do
    def update
      amm_pool = AmmPool.find(params[:id])
      permitted_params = params.require(:amm_pool).permit(:fee_percentage, :fee_protocol_percentage, :status)

      begin
        result = amm_pool.send_event_update_amm_pool(permitted_params)
        flash[:notice] = 'AMM Pool was successfully updated'
        redirect_to admin_amm_pool_path(amm_pool)
      rescue => e
        flash[:error] = e.message
        redirect_to edit_admin_amm_pool_path(amm_pool)
      end
    end
  end

  filter :pair
  filter :token0
  filter :token1

  index do
    id_column
    column :pair
    column :token0
    column :token1
    column :tick_spacing
    column :fee_percentage do |pool|
      number_to_percentage(pool.fee_percentage * 100, precision: 3)
    end
    column :fee_protocol_percentage do |pool|
      number_to_percentage(pool.fee_protocol_percentage * 100, precision: 3)
    end
    column :price
    column :liquidity
    column :status
    column :created_at
    column :updated_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :pair
      row :token0
      row :token1
      row :tick_spacing
      row :fee_percentage do |pool|
        number_to_percentage(pool.fee_percentage * 100, precision: 3)
      end
      row :fee_protocol_percentage do |pool|
        number_to_percentage(pool.fee_protocol_percentage * 100, precision: 3)
      end
      row :current_tick
      row :sqrt_price
      row :price
      row :liquidity
      row :status
      row :status_explanation
      row :created_at
      row :updated_at

      panel 'Trading Statistics' do
        attributes_table_for resource do
          row :volume_token0
          row :volume_token1
          row :volume_usd
          row :tx_count
        end
      end

      panel 'Token Reserves' do
        attributes_table_for resource do
          row :total_value_locked_token0
          row :total_value_locked_token1
        end
      end

      panel 'Fee Accumulators' do
        attributes_table_for resource do
          row :fee_growth_global0
          row :fee_growth_global1
          row :protocol_fees0
          row :protocol_fees1
        end
      end
    end
  end

  form do |f|
    f.inputs 'AMM Pool Details' do
      if f.object.persisted?
        f.input :status, as: :select, collection: AmmPool.aasm.states_for_select
      else
        render 'admin/amm_pools/tickrate_explanation'
        f.input :pair
        f.input :token0
        f.input :token1
        f.input :tick_spacing
      end
      f.input :fee_percentage, as: :number, input_html: { min: 0, max: 1, step: 0.0001 },
        hint: 'Nhập dưới dạng số thập phân, ví dụ: 0.003 (0.3%), 0.0001 (0.01%), 0.01 (1%)'
      f.input :fee_protocol_percentage, as: :number, input_html: { min: 0, max: 1, step: 0.01 },
        hint: 'Nhập dưới dạng số thập phân, ví dụ: 0.05 (5%)'
      f.actions
    end
  end
end
