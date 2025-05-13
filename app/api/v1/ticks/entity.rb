# frozen_string_literal: true

module V1
  module Ticks
    class Entity < Grape::Entity
      expose :tick_index
      expose :liquidity_gross
      expose :liquidity_net
      expose :fee_growth_outside0
      expose :fee_growth_outside1
      expose :initialized
    end
  end
end
