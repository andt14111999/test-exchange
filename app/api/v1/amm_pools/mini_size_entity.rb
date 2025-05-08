# frozen_string_literal: true

module V1
  module AmmPools
    class MiniSizeEntity < Grape::Entity
      expose :pair
      expose :token0
      expose :token1
    end
  end
end
