# frozen_string_literal: true

module Api
  module V1
    class BalancesController < BaseController
      before_action :authenticate_user!

      def index
        balances = {
          coin_accounts: CoinAccount::SUPPORTED_NETWORKS.keys.map do |coin_type|
            main_account = current_user.coin_accounts.main.of_coin(coin_type).first
            deposit_accounts = current_user.coin_accounts.deposit.of_coin(coin_type)

            {
              coin_type: coin_type,
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
          fiat_accounts: FiatAccount::SUPPORTED_CURRENCIES.keys.map do |currency|
            account = current_user.fiat_accounts.find_by(currency: currency)
            {
              currency: currency,
              currency_name: FiatAccount::SUPPORTED_CURRENCIES[currency],
              balance: account&.balance || 0,
              frozen_balance: account&.frozen_balance || 0
            }
          end
        }

        render json: { status: 'success', data: balances }
      end
    end
  end
end
