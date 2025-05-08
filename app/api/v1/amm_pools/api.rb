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

        desc 'Get active AMM pools with basic information'
        get :active do
          pools = ::AmmPool.active.select(:pair, :token0, :token1)
          present :pools, pools, with: V1::AmmPools::MiniSizeEntity
        end

        desc 'Get an AMM pool by pair'
        params do
          requires :pair, type: String, desc: 'AMM Pool pair (e.g. BTC/USDT)'
        end
        get ':pair' do
          pool = ::AmmPool.active.find_by(pair: params[:pair])
          error!({ error: 'AMM Pool not found' }, :not_found) unless pool

          present pool, with: V1::AmmPools::Entity
        end
      end
    end
  end
end
