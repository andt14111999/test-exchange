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
              main_account = current_user.coin_accounts.main.of_coin(coin_currency).first
              deposit_accounts = current_user.coin_accounts.deposit.of_coin(coin_currency)

              {
                coin_currency: coin_currency,
                main: {
                  balance: main_account&.balance || 0,
                  frozen_balance: main_account&.frozen_balance || 0
                },
                deposit_accounts: deposit_accounts.map do |account|
                  {
                    layer: account.layer,
                    address: account.address,
                    balance: account.balance,
                    frozen_balance: account.frozen_balance
                  }
                end
              }
            end,
            fiat_accounts: ::FiatAccount::SUPPORTED_CURRENCIES.keys.map do |currency|
              account = current_user.fiat_accounts.find_by(currency: currency)
              {
                currency: currency,
                currency_name: ::FiatAccount::SUPPORTED_CURRENCIES[currency],
                balance: account&.balance || 0,
                frozen_balance: account&.frozen_balance || 0
              }
            end
          }

          present({ status: 'success', data: balances })
        end
      end
    end
  end
end
