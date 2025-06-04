# frozen_string_literal: true

module V1
  module Balances
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :balances do
        desc 'Get user balances'
        get do
          balances = {
            coin_accounts: ::CoinAccount::SUPPORTED_NETWORKS.keys.map do |coin_currency|
              main_account = current_user.coin_accounts.of_coin(coin_currency).main
              decimal = CoinConfig.get_decimal(coin_currency)

              {
                coin_currency: coin_currency,
                balance: (main_account&.available_balance || 0).round(decimal),
                frozen_balance: (main_account&.frozen_balance || 0).round(decimal)
              }
            end,
            fiat_accounts: ::FiatAccount::SUPPORTED_CURRENCIES.keys.map do |currency|
              account = current_user.fiat_accounts.find_by(currency: currency)
              decimal = CoinConfig.get_decimal(currency)

              {
                currency: currency,
                balance: (account&.available_balance || 0).round(decimal),
                frozen_balance: (account&.frozen_balance || 0).round(decimal)
              }
            end
          }

          present({ status: 'success', data: balances })
        end
      end
    end
  end
end
