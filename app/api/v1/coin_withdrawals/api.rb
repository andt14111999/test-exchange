# frozen_string_literal: true

module V1
  module CoinWithdrawals
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :coin_withdrawals do
        desc 'Create a new coin withdrawal'
        params do
          requires :coin_address, type: String, desc: 'Recipient address'
          requires :coin_amount, type: BigDecimal, desc: 'Amount to withdraw'
          requires :coin_currency, type: String, desc: 'Coin currency (e.g. usdt, eth)'
          requires :coin_layer, type: String, desc: 'Network layer (e.g. erc20, bep20)'
        end

        post do
          # Create withdrawal with the params
          withdrawal = CoinWithdrawal.new(
            user: current_user,
            coin_currency: params[:coin_currency].downcase,
            coin_amount: params[:coin_amount],
            coin_address: params[:coin_address],
            coin_layer: params[:coin_layer]
          )

          if withdrawal.save
            present({
              status: 'success',
              data: {
                id: withdrawal.id,
                coin_currency: withdrawal.coin_currency,
                coin_amount: withdrawal.coin_amount,
                coin_fee: withdrawal.coin_fee,
                coin_address: withdrawal.coin_address,
                coin_layer: withdrawal.coin_layer,
                status: withdrawal.status,
                created_at: withdrawal.created_at
              }
            })
          else
            error!({
              status: 'error',
              message: withdrawal.errors.full_messages.join(', ')
            }, 422)
          end
        end

        desc 'Get coin withdrawal details by ID'
        params do
          requires :id, type: Integer, desc: 'Coin withdrawal ID'
        end

        route_param :id do
          get do
            withdrawal = current_user.coin_withdrawals.find_by(id: params[:id])

            unless withdrawal
              error!({
                status: 'error',
                message: 'Coin withdrawal not found'
              }, 404)
            end

            present({
              status: 'success',
              data: {
                id: withdrawal.id,
                coin_currency: withdrawal.coin_currency,
                coin_amount: withdrawal.coin_amount,
                coin_fee: withdrawal.coin_fee,
                coin_address: withdrawal.coin_address,
                coin_layer: withdrawal.coin_layer,
                status: withdrawal.status,
                tx_hash: withdrawal.tx_hash,
                created_at: withdrawal.created_at,
                updated_at: withdrawal.updated_at
              }
            })
          end
        end
      end
    end
  end
end
