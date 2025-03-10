# frozen_string_literal: true

module V1
  module Balances
    class Entity < Grape::Entity
      expose :status
      expose :data do
        expose :coin_accounts, using: V1::CoinAccount::Entity
        expose :fiat_accounts, using: V1::FiatAccount::Entity
      end
    end
  end
end
