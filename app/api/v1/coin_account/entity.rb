# frozen_string_literal: true

module V1
  module CoinAccount
    class Entity < Grape::Entity
      expose :coin_type
      expose :main do
        expose :balance
        expose :frozen_balance
      end
      expose :deposit_accounts do
        expose :layer
        expose :address
        expose :balance
        expose :frozen_balance
      end
    end
  end
end
