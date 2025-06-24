# frozen_string_literal: true

require_relative 'entity'

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

      resource :coin_settings do
        desc 'Get all coin settings'
        get do
          settings = CoinSetting.order(:currency)
          present settings, with: V1::CoinSettings::CoinSettingEntity
        end
      end
    end
  end
end
