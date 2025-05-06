# frozen_string_literal: true

require_relative 'entity'

module V1
  module FiatWithdrawals
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :fiat_withdrawals do
        desc 'Get all fiat withdrawals'
        params do
          optional :status, type: String, values: FiatWithdrawal::STATUSES, desc: 'Filter by status'
          optional :currency, type: String, desc: 'Filter by currency'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 20, desc: 'Items per page'
        end
        get do
          withdrawals = current_user.fiat_withdrawals
                               .includes(:fiat_account)
                               .order(created_at: :desc)

          # Apply filters
          withdrawals = withdrawals.where(status: params[:status]) if params[:status].present?
          withdrawals = withdrawals.of_currency(params[:currency]) if params[:currency].present?

          # Paginate
          withdrawals = withdrawals.page(params[:page]).per(params[:per_page])

          present withdrawals, with: V1::FiatWithdrawals::Entity
        end

        desc 'Get withdrawal details'
        params do
          requires :id, type: String, desc: 'Withdrawal ID'
        end
        get ':id' do
          withdrawal = current_user.fiat_withdrawals.find(params[:id])
          present withdrawal, with: V1::FiatWithdrawals::FiatWithdrawalDetail
        end

        desc 'Create a new withdrawal'
        params do
          requires :currency, type: String, desc: 'Fiat currency (e.g., VND, PHP)'
          requires :country_code, type: String, desc: 'Country code'
          requires :fiat_amount, type: BigDecimal, desc: 'Withdrawal amount'
          requires :bank_name, type: String, desc: 'Bank name'
          requires :bank_account_name, type: String, desc: 'Bank account name'
          requires :bank_account_number, type: String, desc: 'Bank account number'
          optional :bank_branch, type: String, desc: 'Bank branch'
          optional :fiat_account_id, type: String, desc: 'Fiat account ID (if not provided, default account for currency will be used)'
        end
        post do
          # Find fiat account
          fiat_account = if params[:fiat_account_id].present?
                          current_user.fiat_accounts.find(params[:fiat_account_id])
          else
                          current_user.fiat_accounts.find_by(currency: params[:currency])
          end

          # Check if fiat account exists
          unless fiat_account
            error!({ error: 'Fiat account not found for this currency' }, 404)
          end

          # Check if fiat account currency matches requested currency
          if fiat_account.currency != params[:currency]
            error!({ error: 'Fiat account currency does not match withdrawal currency' }, 400)
          end

          # Check if withdrawal amount is valid
          min_amount = Rails.application.config.min_withdrawal_amounts[params[:currency]].to_d
          max_amount = Rails.application.config.max_withdrawal_amounts[params[:currency]].to_d

          if params[:fiat_amount] < min_amount
            error!({ error: "Withdrawal amount must be at least #{min_amount} #{params[:currency]}" }, 400)
          end

          if params[:fiat_amount] > max_amount
            error!({ error: "Withdrawal amount cannot exceed #{max_amount} #{params[:currency]}" }, 400)
          end

          # Check daily and weekly limits
          daily_limit = Rails.application.config.withdrawal_daily_limits[params[:currency]].to_d
          weekly_limit = Rails.application.config.withdrawal_weekly_limits[params[:currency]].to_d

          today_total = current_user.fiat_withdrawals
                             .of_currency(params[:currency])
                             .today
                             .sum(:fiat_amount)

          if today_total + params[:fiat_amount] > daily_limit
            error!({ error: "This withdrawal would exceed your daily limit of #{daily_limit} #{params[:currency]}" }, 400)
          end

          this_week_total = current_user.fiat_withdrawals
                                 .of_currency(params[:currency])
                                 .this_week
                                 .sum(:fiat_amount)

          if this_week_total + params[:fiat_amount] > weekly_limit
            error!({ error: "This withdrawal would exceed your weekly limit of #{weekly_limit} #{params[:currency]}" }, 400)
          end

          # Check if user has sufficient balance
          if params[:fiat_amount] > fiat_account.available_balance
            error!({ error: "Insufficient balance. Available: #{fiat_account.available_balance} #{params[:currency]}" }, 400)
          end

          # Create the withdrawal
          withdrawal = FiatWithdrawal.new(
            user_id: current_user.id,
            fiat_account_id: fiat_account.id,
            currency: params[:currency],
            country_code: params[:country_code],
            fiat_amount: params[:fiat_amount],
            bank_name: params[:bank_name],
            bank_account_name: params[:bank_account_name],
            bank_account_number: params[:bank_account_number],
            bank_branch: params[:bank_branch],
            status: 'pending'
          )

          if withdrawal.save
            # Processing will start automatically in the background
            withdrawal.mark_as_processing!
            present withdrawal, with: V1::FiatWithdrawals::FiatWithdrawalDetail
          else
            error!({ error: withdrawal.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Cancel withdrawal'
        params do
          requires :id, type: String, desc: 'Withdrawal ID'
          optional :cancel_reason, type: String, desc: 'Reason for cancellation'
        end
        put ':id/cancel' do
          withdrawal = current_user.fiat_withdrawals.find(params[:id])

          # Check if withdrawal can be cancelled
          unless withdrawal.can_be_cancelled?
            error!({ error: 'This withdrawal cannot be cancelled in its current state' }, 400)
          end

          if withdrawal.cancel!(params[:cancel_reason] || 'Cancelled by user')
            present withdrawal, with: V1::FiatWithdrawals::FiatWithdrawalDetail
          else
            error!({ error: 'Failed to cancel withdrawal' }, 422)
          end
        end

        desc 'Retry a rejected withdrawal'
        params do
          requires :id, type: String, desc: 'Withdrawal ID'
          optional :bank_name, type: String, desc: 'Updated bank name'
          optional :bank_account_name, type: String, desc: 'Updated bank account name'
          optional :bank_account_number, type: String, desc: 'Updated bank account number'
          optional :bank_branch, type: String, desc: 'Updated bank branch'
        end
        put ':id/retry' do
          withdrawal = current_user.fiat_withdrawals.find(params[:id])

          # Check if withdrawal can be retried
          unless withdrawal.bank_rejected? && withdrawal.can_be_retried?
            error!({ error: 'This withdrawal cannot be retried' }, 400)
          end

          # Update bank details if provided
          update_params = {}
          update_params[:bank_name] = params[:bank_name] if params[:bank_name].present?
          update_params[:bank_account_name] = params[:bank_account_name] if params[:bank_account_name].present?
          update_params[:bank_account_number] = params[:bank_account_number] if params[:bank_account_number].present?
          update_params[:bank_branch] = params[:bank_branch] if params[:bank_branch].present?

          withdrawal.update(update_params) if update_params.present?

          if withdrawal.retry!
            present withdrawal, with: V1::FiatWithdrawals::FiatWithdrawalDetail
          else
            error!({ error: 'Failed to retry withdrawal' }, 422)
          end
        end

        # For tracking P2P withdrawals
        desc 'Get P2P withdrawal status'
        params do
          requires :trade_id, type: String, desc: 'Trade ID'
        end
        get 'p2p/:trade_id' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:trade_id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Check if user is part of this trade
          unless [ trade.buyer_id, trade.seller_id ].include?(current_user.id)
            error!({ error: 'Unauthorized access to this trade' }, 403)
          end

          # Check if this is a withdrawal trade
          unless trade.is_fiat_token_withdrawal_trade?
            error!({ error: 'This trade does not have an associated withdrawal' }, 400)
          end

          withdrawal = trade.fiat_withdrawal

          error!({ error: 'No withdrawal found for this trade' }, 404) unless withdrawal

          present withdrawal, with: V1::FiatWithdrawals::FiatWithdrawalDetail
        end
      end
    end
  end
end
