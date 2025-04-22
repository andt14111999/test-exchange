# frozen_string_literal: true

module V1
  module Coins
    class Entity < Grape::Entity
      expose :coins, documentation: { type: 'Array', desc: 'List of supported coins' }
      expose :fiats, documentation: { type: 'Array', desc: 'List of supported fiats' }
    end
  end
end
