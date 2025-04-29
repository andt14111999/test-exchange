# frozen_string_literal: true

module V1
  module Trades
    class Entity < Grape::Entity
      expose :id
      expose :ref
      expose :coin_currency
      expose :fiat_currency
      expose :coin_amount
      expose :fiat_amount
      expose :price
      expose :payment_method do |trade|
        trade.offer&.payment_method&.display_name
      end
      expose :taker_side
      expose :status
      expose :has_payment_proof
      expose :payment_proof_status
      expose :paid_at
      expose :released_at
      expose :cancelled_at
      expose :disputed_at
      expose :expired_at
      expose :created_at
      expose :buyer_id
      expose :seller_id
      expose :offer_id
      expose :is_fiat_token_trade do |trade|
        trade.fiat_token_trade?
      end
      expose :payment_type do |trade|
        if trade.is_fiat_token_deposit_trade?
          'FiatDeposit'
        elsif trade.is_fiat_token_withdrawal_trade?
          'FiatWithdrawal'
        else
          nil
        end
      end
      expose :payment_id do |trade|
        if trade.is_fiat_token_deposit_trade?
          trade.fiat_token_deposit_id
        elsif trade.is_fiat_token_withdrawal_trade?
          trade.fiat_token_withdrawal_id
        end
      end
    end

    class TradeDetail < Entity
      expose :payment_details
      expose :payment_receipt_details
      expose :dispute_reason
      expose :dispute_resolution
      expose :is_deposit_trade do |trade|
        trade.is_fiat_token_deposit_trade?
      end
      expose :is_withdrawal_trade do |trade|
        trade.is_fiat_token_withdrawal_trade?
      end
      expose :fee_ratio
      expose :coin_trading_fee
      expose :amount_after_fee do |trade|
        trade.coin_amount - trade.coin_trading_fee
      end
      expose :time_left_seconds do |trade|
        trade.payment_time_left
      end
      expose :buyer, using: V1::Users::Entity
      expose :seller, using: V1::Users::Entity
      expose :updated_at

      # Fiat token information
      expose :fiat_token do |trade, options|
        if trade.is_fiat_token_deposit_trade?
          {
            type: 'deposit',
            data: V1::FiatDeposits::Entity.represent(trade.fiat_token_deposit, options)
          }
        elsif trade.is_fiat_token_withdrawal_trade?
          {
            type: 'withdrawal',
            data: V1::FiatWithdrawals::Entity.represent(trade.fiat_token_withdrawal, options)
          }
        else
          nil
        end
      end

      # Permission helpers
      expose :can_be_released_by_current_user do |trade, options|
        current_user = options[:current_user]
        trade.can_be_released_by?(current_user)
      end

      expose :can_be_disputed_by_current_user do |trade, options|
        current_user = options[:current_user]
        trade.can_be_disputed_by?(current_user)
      end

      expose :can_be_cancelled_by_current_user do |trade, options|
        current_user = options[:current_user]
        trade.can_be_cancelled_by?(current_user)
      end

      expose :can_be_marked_paid_by_current_user do |trade, options|
        current_user = options[:current_user]
        trade.can_be_marked_paid_by?(current_user)
      end
    end

    class TradeDetailWithMessages < TradeDetail
      expose :messages, using: V1::Messages::Entity do |trade|
        trade.messages.order(created_at: :desc).limit(20)
      end
    end
  end
end
