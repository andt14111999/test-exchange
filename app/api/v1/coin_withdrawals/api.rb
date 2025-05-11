# frozen_string_literal: true

module V1
  module CoinWithdrawals
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :coin_withdrawals do
        desc 'Create a new coin withdrawal'
        params do
          optional :coin_address, type: String, desc: 'Recipient address (required for external withdrawals)'
          requires :coin_amount, type: BigDecimal, desc: 'Amount to withdraw'
          requires :coin_currency, type: String, desc: 'Coin currency (e.g. usdt, eth)'
          optional :coin_layer, type: String, desc: 'Network layer (e.g. erc20, bep20, required for external withdrawals)'
          optional :receiver_email, type: String, desc: 'Email of recipient for internal transfers'

          mutually_exclusive :coin_address, :receiver_email
          at_least_one_of :coin_address, :receiver_email

          given :coin_address do
            requires :coin_layer, type: String, desc: 'Network layer is required for external withdrawals'
          end
        end

        post do
          # Create withdrawal with the params
          withdrawal = CoinWithdrawal.new(
            user: current_user,
            coin_currency: params[:coin_currency].downcase,
            coin_amount: params[:coin_amount],
            coin_address: params[:coin_address],
            coin_layer: params[:coin_layer],
            receiver_email: params[:receiver_email]
          )

          if withdrawal.save
            present({
              status: 'success',
              data: present(withdrawal, with: Entity)
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
              data: present(withdrawal, with: Entity)
            })
          end
        end
      end
    end
  end
end
