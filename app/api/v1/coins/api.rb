# frozen_string_literal: true

module V1
  module Coins
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      resource :coins do
        desc 'Get supported coins and fiats'
        get do
          present({
            coins: CoinConfig.coins,
            fiats: CoinConfig.fiats
          }, with: V1::Coins::Entity)
        end
      end
    end
  end
end
