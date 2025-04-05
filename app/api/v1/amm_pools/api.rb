# frozen_string_literal: true

module V1
  module AmmPools
    class Api < Grape::API
      resource :amm_pools do
        desc 'Get all AMM pools'
        get do
          pools = ::AmmPool.active

          present :amm_pools, pools, with: V1::AmmPools::Entity
          present :meta, generate_meta(pools), with: Api::EntityMeta
        end

        desc 'Get an AMM pool by ID'
        params do
          requires :id, type: Integer, desc: 'AMM Pool ID'
        end
        get ':id' do
          pool = ::AmmPool.find_by(id: params[:id])
          error!({ error: 'AMM Pool not found' }, :not_found) unless pool

          present pool, with: V1::AmmPools::Entity
        end
      end
    end
  end
end
