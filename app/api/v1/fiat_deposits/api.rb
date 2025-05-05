# frozen_string_literal: true

require_relative 'entity'

module V1
  module FiatDeposits
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :fiat_deposits do
        desc 'Get all fiat deposits'
        params do
          optional :status, type: String, values: FiatDeposit::STATUSES, desc: 'Filter by status'
          optional :currency, type: String, desc: 'Filter by currency'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 20, desc: 'Items per page'
        end
        get do
          deposits = current_user.fiat_deposits
                           .includes(:fiat_account, :payable)
                           .order(created_at: :desc)

          # Apply filters
          deposits = deposits.where(status: params[:status]) if params[:status].present?
          deposits = deposits.of_currency(params[:currency]) if params[:currency].present?

          # Paginate
          deposits = deposits.page(params[:page]).per(params[:per_page])

          present deposits, with: V1::FiatDeposits::Entity
        end

        desc 'Get deposit details'
        params do
          requires :id, type: String, desc: 'Deposit ID'
        end
        get ':id' do
          deposit = current_user.fiat_deposits.includes(:payable).find(params[:id])
          present deposit, with: V1::FiatDeposits::FiatDepositDetail
        end

        desc 'Create a new deposit'
        params do
          requires :currency, type: String, desc: 'Fiat currency (e.g., VND, PHP)'
          requires :country_code, type: String, desc: 'Country code'
          requires :fiat_amount, type: BigDecimal, desc: 'Deposit amount'
          requires :offer_id, type: String, desc: 'ID of the offer to use for this deposit'
          optional :fiat_account_id, type: String, desc: 'Fiat account ID (if not provided, default account for currency will be used)'
          optional :memo, type: String, desc: 'Deposit memo/reference'
        end
        post do
          # Find the specified offer
          offer = Offer.find(params[:offer_id])

          # Check if the offer is valid and active
          unless offer.active?
            error!({ error: 'Offer is not active' }, 400)
          end

          # Check if user is trying to create deposit with their own offer
          if offer.user_id == current_user.id
            error!({ error: 'Cannot create deposit with your own offer' }, 400)
          end

          # Check if offer currency matches requested currency
          if offer.currency != params[:currency]
            error!({ error: 'Offer currency does not match deposit currency' }, 400)
          end

          # Find or create fiat account
          fiat_account = if params[:fiat_account_id].present?
                          current_user.fiat_accounts.find(params[:fiat_account_id])
          else
                          current_user.fiat_accounts.find_by(currency: params[:currency]) ||
                          current_user.fiat_accounts.create!(
                            currency: params[:currency],
                            country_code: params[:country_code],
                            balance: 0,
                            frozen_balance: 0
                          )
          end

          # Check if fiat account currency matches requested currency
          if fiat_account.currency != params[:currency]
            error!({ error: 'Fiat account currency does not match deposit currency' }, 400)
          end

          # Create the deposit and trade in a transaction
          ActiveRecord::Base.transaction do
            # Calculate coin amount based on offer price
            market_price = offer.price
            coin_amount = params[:fiat_amount] / market_price

            # Use system configuration for fee ratio or default to 1%
            fee_ratio = Rails.application.config.trading_fees[offer.coin_currency] rescue 0.01

            # Create trade first
            trade = Trade.new(
              buyer_id: current_user.id,
              seller_id: offer.user_id,
              offer_id: offer.id,
              coin_currency: offer.coin_currency,
              fiat_currency: params[:currency],
              coin_amount: coin_amount,
              fiat_amount: params[:fiat_amount],
              price: market_price,
              fee_ratio: fee_ratio,
              coin_trading_fee: coin_amount * fee_ratio,
              payment_method: offer.payment_method || 'bank_transfer',
              taker_side: 'buy',
              status: 'awaiting'
            )

            if trade.save
              # Create deposit associated with the trade
              deposit = FiatDeposit.new(
                user_id: current_user.id,
                fiat_account_id: fiat_account.id,
                currency: params[:currency],
                country_code: params[:country_code],
                fiat_amount: params[:fiat_amount],
                memo: params[:memo],
                status: 'awaiting',
                payable: trade
              )

              if deposit.save
                # Update trade with deposit reference
                trade.update!(fiat_token_deposit_id: deposit.id)
                # Mark as pending immediately to start the deposit process
                deposit.mark_as_pending!
                present deposit, with: V1::FiatDeposits::FiatDepositDetail
              else
                error!({ error: deposit.errors.full_messages.join(', ') }, 422)
              end
            else
              error!({ error: trade.errors.full_messages.join(', ') }, 422)
            end
          end
        end

        desc 'Mark deposit as money sent'
        params do
          requires :id, type: String, desc: 'Deposit ID'
          optional :payment_proof_url, type: String, desc: 'URL to payment proof/receipt'
          optional :payment_description, type: String, desc: 'Additional payment info'
          optional :mark_as_sent, type: Boolean, default: true, desc: 'Whether to mark the deposit as money sent or just save payment proof'
          optional :additional_proof, type: Boolean, default: false, desc: 'Whether this is an additional proof for an existing money_sent deposit'
        end
        put ':id/money_sent' do
          deposit = current_user.fiat_deposits.includes(:payable).find(params[:id])
          trade = deposit.payable

          ActiveRecord::Base.transaction do
            # Handle additional proof for money_sent deposits
            if params[:additional_proof] && deposit.money_sent?
              if params[:payment_proof_url].present?
                deposit.update!(
                  payment_proof_url: params[:payment_proof_url],
                  payment_description: deposit.payment_description.to_s + "\n" + params[:payment_description].to_s
                )
                trade.add_payment_proof!(params[:payment_description])
              end

              present deposit, with: V1::FiatDeposits::FiatDepositDetail
              return
            end

            # Regular flow - check deposit state
            unless deposit.pending?
              error!({ error: 'Deposit must be in pending state to mark as money sent' }, 400)
            end

            # Update payment proof if provided
            if params[:payment_proof_url].present?
              deposit.update!(
                payment_proof_url: params[:payment_proof_url],
                payment_description: params[:payment_description]
              )
              trade.add_payment_proof!(params[:payment_description])
            end

            # Mark as money sent if requested
            if params[:mark_as_sent]
              deposit.money_sent!

              # If deposit is successfully marked as money sent, update trade status
              if deposit.money_sent?
                trade.mark_as_paid! if trade.may_mark_as_paid?
                present deposit, with: V1::FiatDeposits::FiatDepositDetail
              else
                error!({ error: 'Failed to mark deposit as money sent' }, 422)
              end
            else
              present deposit, with: V1::FiatDeposits::FiatDepositDetail
            end
          end
        end

        desc 'Cancel deposit'
        params do
          requires :id, type: String, desc: 'Deposit ID'
          optional :cancel_reason, type: String, desc: 'Reason for cancellation'
        end
        put ':id/cancel' do
          deposit = current_user.fiat_deposits.includes(:payable).find(params[:id])
          trade = deposit.payable

          ActiveRecord::Base.transaction do
            # Check if deposit can be cancelled
            unless %w[awaiting pending].include?(deposit.status)
              error!({ error: 'Only deposits in awaiting or pending state can be cancelled' }, 400)
            end

            # Set cancel reason before calling cancel!
            cancel_reason = params[:cancel_reason] || 'Cancelled by user'
            deposit.cancel_reason = cancel_reason

            if deposit.cancel!
              # Cancel the associated trade
              trade.cancel! if trade.may_cancel?
              present deposit, with: V1::FiatDeposits::FiatDepositDetail
            else
              error!({ error: 'Failed to cancel deposit' }, 422)
            end
          end
        end

        desc 'Submit ownership proof'
        params do
          requires :id, type: String, desc: 'Deposit ID'
          requires :ownership_proof_url, type: String, desc: 'URL to ownership proof document'
          requires :sender_name, type: String, desc: 'Name of the sender'
          requires :sender_account_number, type: String, desc: 'Account number of the sender'
        end
        put ':id/verify_ownership' do
          deposit = current_user.fiat_deposits.includes(:payable).find(params[:id])

          # Check if ownership verification is needed
          unless %w[ownership_verifying locked_due_to_unverified_ownership].include?(deposit.status)
            error!({ error: 'Deposit does not require ownership verification' }, 400)
          end

          if deposit.verify_ownership!(
            params[:ownership_proof_url],
            params[:sender_name],
            params[:sender_account_number]
          )
            present deposit, with: V1::FiatDeposits::FiatDepositDetail
          else
            error!({ error: 'Failed to verify ownership' }, 422)
          end
        end
      end
    end
  end
end
