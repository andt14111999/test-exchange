# frozen_string_literal: true

module V1
  module CoinWithdrawals
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper
      helpers V1::Helpers::DeviceHelper
      helpers V1::Helpers::TwoFactorHelper

      before { authenticate_user! }

      resource :coin_withdrawals do
        desc 'Create a new coin withdrawal'
        params do
          optional :coin_address, type: String, desc: 'Recipient address (required for external withdrawals)'
          requires :coin_amount, type: BigDecimal, desc: 'Amount to withdraw'
          requires :coin_currency, type: String, desc: 'Coin currency (e.g. usdt, eth)'
          optional :coin_layer, type: String, desc: 'Network layer (e.g. erc20, bep20, required for external withdrawals)'
          optional :receiver_email, type: String, desc: 'Email of recipient for internal transfers'
          optional :receiver_username, type: String, desc: 'Username of recipient for internal transfers'
          optional :two_factor_code, type: String, desc: '2FA code (required if 2FA enabled and device not trusted)'

          mutually_exclusive :coin_address, :receiver_email, :receiver_username
          at_least_one_of :coin_address, :receiver_email, :receiver_username

          given :coin_address do
            requires :coin_layer, type: String, desc: 'Network layer is required for external withdrawals'
          end
        end

        post do
          # Verify 2FA if required
          verify_2fa_if_required!
          # Create withdrawal with the params
          withdrawal = CoinWithdrawal.new(
            user: current_user,
            coin_currency: params[:coin_currency].downcase,
            coin_amount: params[:coin_amount],
            coin_address: params[:coin_address],
            coin_layer: params[:coin_layer],
            receiver_email: params[:receiver_email],
            receiver_username: params[:receiver_username]
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

        desc 'Check if receiver exists in system'
        params do
          requires :receiver_username, type: String, desc: 'Username of receiver to check'
        end

        get :check_receiver do
          # If receiver is the current user, return false
          if current_user.username == params[:receiver_username]
            body(false.to_json)
          else
            user_exists = User.exists?(username: params[:receiver_username])
            body(user_exists.to_json)
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
