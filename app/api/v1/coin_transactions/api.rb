# frozen_string_literal: true

module V1
  module CoinTransactions
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :coin_transactions do
        desc 'Get coin transactions (deposits and withdrawals) for a specific currency'
        params do
          requires :coin_currency, type: String, desc: 'Coin currency (e.g. usdt, eth)'
          optional :page, type: Integer, default: 1, desc: 'Page number for pagination'
          optional :per_page, type: Integer, default: 20, desc: 'Number of items per page'
        end

        get do
          coin_accounts = current_user.coin_accounts.where(coin_currency: params[:coin_currency])

          if coin_accounts.exists?
            deposits = CoinDeposit
              .where(coin_account: coin_accounts)
              .includes(:coin_deposit_operation)
              .order(created_at: :desc)
              .page(params[:page])
              .per(params[:per_page])

            withdrawals = CoinWithdrawal
              .where(user: current_user, coin_currency: params[:coin_currency])
              .includes(:coin_withdrawal_operation)
              .order(created_at: :desc)
              .page(params[:page])
              .per(params[:per_page])

            present({
              status: 'success',
              data: {
                deposits: deposits.map do |deposit|
                  {
                    id: deposit.id,
                    amount: deposit.coin_amount,
                    coin_currency: deposit.coin_currency,
                    status: deposit.status,
                    hash: deposit.tx_hash,
                    created_at: deposit.created_at,
                    updated_at: deposit.updated_at
                  }
                end,
                withdrawals: withdrawals.map do |withdrawal|
                  {
                    id: withdrawal.id,
                    amount: withdrawal.coin_amount,
                    coin_currency: withdrawal.coin_currency,
                    status: withdrawal.status,
                    hash: withdrawal.tx_hash,
                    address: withdrawal.coin_address,
                    created_at: withdrawal.created_at,
                    updated_at: withdrawal.updated_at
                  }
                end,
                pagination: {
                  deposits: {
                    current_page: deposits.current_page,
                    total_pages: deposits.total_pages,
                    total_count: deposits.total_count,
                    per_page: deposits.limit_value
                  },
                  withdrawals: {
                    current_page: withdrawals.current_page,
                    total_pages: withdrawals.total_pages,
                    total_count: withdrawals.total_count,
                    per_page: withdrawals.limit_value
                  }
                }
              }
            })
          else
            error!({
              status: 'error',
              message: 'No coin accounts found for this currency'
            }, 404)
          end
        end
      end
    end
  end
end
