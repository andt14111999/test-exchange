# frozen_string_literal: true

module V1
  module AmmOrders
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before do
        authenticate_user!
      end

      resource :amm_orders do
        desc 'Get a list of orders for the current user'
        params do
          optional :status, type: String, values: %w[pending processing success error all], default: 'success', desc: 'Filter by status'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 10, desc: 'Items per page'
        end
        get do
          options = params[:status] == 'all' ? {} : { status: params[:status] }.compact
          orders = current_user.amm_orders.where(options).order(created_at: :desc).page(params[:page]).per(params[:per_page])

          present :amm_orders, orders, with: V1::AmmOrders::Entity
          present :meta, generate_meta(orders), with: Api::EntityMeta
        end

        desc 'Create a new order'
        params do
          requires :pool_pair, type: String, desc: 'Pool pair'
          requires :zero_for_one, type: Boolean, desc: 'Direction of swap (true for token0 to token1, false for token1 to token0)'
          requires :amount_specified, type: BigDecimal, desc: 'Amount to swap'
          requires :amount_estimated, type: BigDecimal, desc: 'Estimated amount'
          optional :slippage, type: BigDecimal, default: 0.05, desc: 'Slippage tolerance'
        end
        post do
          pool = ::AmmPool.find_by(pair: params[:pool_pair])
          error!({ error: 'Pool not found' }, 404) unless pool

          order = current_user.amm_orders.new(
            amm_pool: pool,
            zero_for_one: params[:zero_for_one],
            amount_specified: params[:amount_specified],
            amount_estimated: params[:amount_estimated],
            slippage: params[:slippage]
          )

          order.generate_identifier

          if order.save
            present order, with: V1::AmmOrders::Entity
          else
            error!({ error: order.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Get an order by ID'
        params do
          requires :id, type: Integer, desc: 'Order ID'
        end
        get ':id' do
          order = current_user.amm_orders.find_by(id: params[:id])
          error!({ error: 'Order not found' }, 404) unless order

          present order, with: V1::AmmOrders::Entity
        end
      end
    end
  end
end
