# frozen_string_literal: true

module V1
  module CoinAccounts
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :coin_accounts do
        desc 'Get coin address for a specific coin account'
        params do
          requires :coin_currency, type: String, desc: 'Coin currency (e.g. usdt, eth)'
          requires :layer, type: String, desc: 'Network layer (e.g. erc20, bep20)'
        end

        get :address do
          coin_account = current_user.coin_accounts.find_by(
            coin_currency: params[:coin_currency],
            layer: params[:layer]
          )

          if coin_account
            present({
              status: 'success',
              data: {
                coin_currency: coin_account.coin_currency,
                layer: coin_account.layer,
                address: coin_account.address
              }
            })
          else
            error!({
              status: 'error',
              message: 'Coin account not found'
            }, 404)
          end
        end
      end
    end
  end
end
