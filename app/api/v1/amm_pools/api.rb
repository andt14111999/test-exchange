# frozen_string_literal: true

module V1
  module AmmPools
    class Api < Grape::API
      resource :amm_pools do
        desc 'Get all AMM pools'
        params do
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 10, desc: 'Number of items per page'
        end
        get do
          pools = ::AmmPool.active.page(params[:page]).per(params[:per_page])

          present :amm_pools, pools, with: V1::AmmPools::Entity
          present :meta, generate_meta(pools), with: Api::EntityMeta
        end

        desc 'Get an AMM pool by ID'
        params do
          requires :id, type: Integer, desc: 'AMM Pool ID'
        end
        get ':id' do
          pool = ::AmmPool.active.find_by(id: params[:id])
          error!({ error: 'AMM Pool not found' }, :not_found) unless pool

          present pool, with: V1::AmmPools::Entity
        end
      end
    end
  end
end
