# frozen_string_literal: true

module V1
  module AmmPositions
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before do
        authenticate_user!
      end

      resource :amm_positions do
        desc 'Get a list of positions for the current user'
        params do
          optional :status, type: String, values: %w[pending open closed error all], default: 'open', desc: 'Filter by status'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 10, desc: 'Items per page'
        end
        get do
          positions = current_user.amm_positions

          positions = positions.where(status: params[:status]) unless params[:status] == 'all'

          paginated_positions = positions.page(params[:page]).per(params[:per_page])

          present :amm_positions, paginated_positions, with: V1::AmmPositions::Entity
          present :meta, generate_meta(paginated_positions), with: Api::EntityMeta
        end

        desc 'Create a new position'
        params do
          requires :pool_pair, type: String, desc: 'Pool pair'
          requires :tick_lower_index, type: Integer, desc: 'Lower tick index'
          requires :tick_upper_index, type: Integer, desc: 'Upper tick index'
          requires :amount0_initial, type: BigDecimal, desc: 'Initial amount of token0'
          requires :amount1_initial, type: BigDecimal, desc: 'Initial amount of token1'
          optional :slippage, type: BigDecimal, default: 1.0, desc: 'Slippage tolerance'
        end
        post do
          pool = ::AmmPool.find_by(pair: params[:pool_pair])
          error!({ error: 'Pool not found' }, 404) unless pool

          position = current_user.amm_positions.new(
            amm_pool: pool,
            tick_lower_index: params[:tick_lower_index],
            tick_upper_index: params[:tick_upper_index],
            amount0_initial: params[:amount0_initial],
            amount1_initial: params[:amount1_initial],
            slippage: params[:slippage]
          )

          position.generate_identifier

          if position.save
            present position, with: V1::AmmPositions::Entity
          else
            error!({ error: position.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Get a position by ID'
        params do
          requires :id, type: Integer, desc: 'Position ID'
        end
        get ':id' do
          position = current_user.amm_positions.find_by(id: params[:id])
          error!({ error: 'Position not found' }, 404) unless position

          # Calculate estimated fees and APR for the position
          position.calculate_est_fee if position.open?

          present position, with: V1::AmmPositions::DetailEntity
        end

        desc 'Collect fee for a position'
        params do
          requires :id, type: Integer, desc: 'Position ID'
        end
        post ':id/collect_fee' do
          position = current_user.amm_positions.find_by(id: params[:id])
          error!({ error: 'Position not found' }, 404) unless position

          if position.open?
            position.collect_fee
            { success: true, message: 'Fee collection initiated' }
          else
            error!({ error: 'Cannot collect fee for a position that is not open' }, 422)
          end
        end

        desc 'Close a position'
        params do
          requires :id, type: Integer, desc: 'Position ID'
        end
        post ':id/close' do
          position = current_user.amm_positions.find_by(id: params[:id])
          error!({ error: 'Position not found' }, 404) unless position

          if position.open?
            position.close_position
            { success: true, message: 'Position closing initiated' }
          else
            error!({ error: 'Cannot close a position that is not open' }, 422)
          end
        end
      end
    end
  end
end
