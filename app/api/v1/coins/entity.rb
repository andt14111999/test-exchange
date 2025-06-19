# frozen_string_literal: true

module V1
  module Coins
    class Entity < Grape::Entity
      expose :coins, documentation: { type: 'Array', desc: 'List of supported coins' }
      expose :fiats, documentation: { type: 'Array', desc: 'List of supported fiats' }
    end
  end

  module CoinSettings
    class CoinSettingEntity < Grape::Entity
      expose :id
      expose :currency
      expose :deposit_enabled
      expose :withdraw_enabled
      expose :swap_enabled
      expose :layers, documentation: { type: 'Array', desc: 'Danh sách các layer và trạng thái' }
      expose :created_at
      expose :updated_at
    end
  end
end
