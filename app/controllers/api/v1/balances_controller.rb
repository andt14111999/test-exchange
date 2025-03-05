# frozen_string_literal: true

module Api
  module V1
    class BalancesController < BaseController
      before_action :authenticate_user!

      def index
        balances = {
          coin_accounts: {
            totals: coin_totals_by_type,
            details: coin_account_details
          },
          fiat_accounts: {
            totals: fiat_totals_by_currency,
            details: fiat_account_details
          }
        }

        render json: { status: 'success', data: balances }
      end

      private

      def coin_totals_by_type
        current_user.coin_accounts
          .group(:coin_type)
          .select(
            'coin_type',
            'COALESCE(SUM(balance), 0) as total_balance',
            'COALESCE(SUM(frozen_balance), 0) as frozen_balance',
            'COALESCE(SUM(available_balance), 0) as available_balance'
          )
          .map do |total|
            {
              coin_type: total.coin_type,
              total_balance: total.total_balance,
              available_balance: total.available_balance,
              frozen_balance: total.frozen_balance
            }
          end
      end

      def fiat_totals_by_currency
        current_user.fiat_accounts
          .group(:currency)
          .select(
            'currency',
            'COALESCE(SUM(balance), 0) as total_balance',
            'COALESCE(SUM(frozen_balance), 0) as frozen_balance',
            'COALESCE(SUM(available_balance), 0) as available_balance'
          )
          .map do |total|
            {
              currency: total.currency,
              currency_name: FiatAccount::SUPPORTED_CURRENCIES[total.currency],
              total_balance: total.total_balance,
              available_balance: total.available_balance,
              frozen_balance: total.frozen_balance
            }
          end
      end

      def coin_account_details
        current_user.coin_accounts.map do |account|
          {
            coin_type: account.coin_type,
            layer: account.layer,
            address: account.address,
            total_balance: account.total_balance,
            available_balance: account.available_balance,
            frozen_balance: account.frozen_balance
          }
        end
      end

      def fiat_account_details
        current_user.fiat_accounts.map do |account|
          {
            currency: account.currency,
            currency_name: FiatAccount::SUPPORTED_CURRENCIES[account.currency],
            total_balance: account.total_balance,
            available_balance: account.available_balance,
            frozen_balance: account.frozen_balance
          }
        end
      end
    end
  end
end
