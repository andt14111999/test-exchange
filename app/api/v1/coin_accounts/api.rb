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

        desc 'Generate a new address for a specific coin account'
        params do
          requires :coin_currency, type: String, desc: 'Coin currency (e.g. usdt, eth)'
          requires :layer, type: String, desc: 'Network layer (e.g. erc20, bep20)'
        end

        post :generate_address do
          base_coin = NetworkConfigurationService.base_coin_for_layer(params[:layer])

          base_account = current_user.coin_accounts.find_or_create_by!(
            coin_currency: base_coin,
            layer: params[:layer],
            account_type: 'deposit'
          )

          address = AddressGenerationService.new(base_account).generate

          if address.present?
            base_account.update!(address: address)

            token_accounts = current_user.coin_accounts.where(
              layer: params[:layer],
              account_type: 'deposit'
            ).where.not(coin_currency: base_coin)

            token_accounts.update_all(address: address) if token_accounts.any?

            present({
              status: 'success',
              data: {
                base_coin: base_coin,
                layer: params[:layer],
                address: address,
                updated_accounts: token_accounts.count + 1
              }
            })
          else
            error!({
              status: 'error',
              message: 'Failed to generate address'
            }, 422)
          end
        end
      end
    end
  end
end
