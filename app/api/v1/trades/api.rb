# frozen_string_literal: true

require_relative 'entity'

module V1
  module Trades
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :trades do
        desc 'Get all trades'
        params do
          optional :status, type: String, values: Trade::STATUSES, desc: 'Filter by status'
          optional :role, type: String, values: %w[buyer seller], desc: 'Filter by role (buyer/seller)'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 20, desc: 'Items per page'
        end
        get do
          # Base query - all trades where the user is either buyer or seller
          trades = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id)
                     .includes(:buyer, :seller, :offer)
                     .order(created_at: :desc)

          # Apply filters
          trades = trades.where(status: params[:status]) if params[:status].present?
          trades = trades.where(buyer_id: current_user.id) if params[:role] == 'buyer'
          trades = trades.where(seller_id: current_user.id) if params[:role] == 'seller'

          # Paginate
          trades = trades.page(params[:page]).per(params[:per_page])

          present trades, with: V1::Trades::Entity
        end

        desc 'Get trade details'
        params do
          requires :id, type: String, desc: 'Trade ID'
        end
        get ':id' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Check if the user is part of this trade
          unless [ trade.buyer_id, trade.seller_id ].include?(current_user.id)
            error!({ error: 'Unauthorized access to this trade' }, 403)
          end

          present trade, with: V1::Trades::TradeDetailWithMessages
        end

        desc 'Create a new trade'
        params do
          requires :offer_id, type: String, desc: 'Offer ID'
          requires :coin_amount, type: BigDecimal, desc: 'Coin amount'
          # For fiat withdrawal trades only
          optional :bank_name, type: String, desc: 'Bank name'
          optional :bank_account_name, type: String, desc: 'Bank account name'
          optional :bank_account_number, type: String, desc: 'Bank account number'
          optional :bank_branch, type: String, desc: 'Bank branch'
        end
        post do
          # Find the offer
          offer = Offer.find_by(id: params[:offer_id])
          error!({ error: 'Offer not found' }, 404) unless offer
          error!({ error: 'Offer is not active' }, 400) unless offer.active?

          # Validate amount constraints
          if params[:coin_amount] < offer.min_amount
            error!({ error: "Amount must be at least #{offer.min_amount}" }, 400)
          end

          if params[:coin_amount] > offer.max_amount
            error!({ error: "Amount cannot exceed #{offer.max_amount}" }, 400)
          end

          if params[:coin_amount] > offer.available_amount
            error!({ error: "Not enough available amount. Maximum available: #{offer.available_amount}" }, 400)
          end

          # Calculate trade details
          fiat_amount = params[:coin_amount] * offer.price
          fee_ratio = Rails.application.config.default_trade_fee_ratio
          coin_trading_fee = params[:coin_amount] * fee_ratio

          # Determine buyer and seller based on offer type
          if offer.buy?
            buyer = User.find(offer.user_id)
            seller = current_user
            taker_side = 'sell'
          else
            buyer = current_user
            seller = User.find(offer.user_id)
            taker_side = 'buy'
          end

          # Transaction to ensure atomicity
          trade = nil
          error_message = nil

          ActiveRecord::Base.transaction do
            # Create the trade with initial status
            trade = Trade.new(
              buyer: buyer,
              seller: seller,
              offer_id: offer.id,
              coin_currency: offer.coin_currency,
              fiat_currency: offer.currency,
              coin_amount: params[:coin_amount],
              fiat_amount: fiat_amount,
              price: offer.price,
              fee_ratio: fee_ratio,
              coin_trading_fee: coin_trading_fee,
              payment_method: offer.payment_method&.name || 'Bank Transfer',
              payment_details: offer.payment_details,
              taker_side: taker_side,
              status: 'unpaid', # Set initial status directly
              expired_at: offer.payment_time.minutes.from_now
            )

            # Note: Fiat deposit/withdrawal creation is removed from here
            # and will be handled by Kafka event handler instead

            unless trade.save
              error_message = trade.errors.full_messages.join(', ')
              raise ActiveRecord::Rollback
            end

            # Send the trade creation event to Kafka
            trade.send_trade_create_to_kafka
          end

          # If transaction failed with error message
          if error_message
            error!({ error: error_message }, 422)
          end

          # If we made it here, everything succeeded
          present trade, with: V1::Trades::TradeDetail
        end

        desc 'Mark trade as paid'
        params do
          requires :id, type: String, desc: 'Trade ID'
          requires :payment_receipt_details, type: Hash, desc: 'Payment receipt details'
        end
        put ':id/mark_paid' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Check if the trade is in the appropriate state
          unless trade.unpaid?
            error!({ error: 'Trade cannot be marked as paid in its current state' }, 400)
          end

          # Check if the user is authorized to mark this trade as paid
          unless trade.can_be_marked_paid_by?(current_user)
            error!({ error: 'You are not authorized to mark this trade as paid' }, 403)
          end

          # Update payment details
          trade.payment_receipt_details = params[:payment_receipt_details]
          trade.has_payment_proof = true

          # Mark as paid
          if trade.mark_as_paid! && trade.save
            present trade, with: V1::Trades::TradeDetail
          else
            error!({ error: trade.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Release funds'
        params do
          requires :id, type: String, desc: 'Trade ID'
        end
        put ':id/release' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Check if the user is authorized to release this trade
          unless trade.can_be_released_by?(current_user)
            error!({ error: 'You are not authorized to release funds for this trade' }, 403)
          end

          # Create a trade service
          service = TradeService.new(trade)

          # Release the funds
          if service.release_trade!
            present trade, with: V1::Trades::TradeDetail
          else
            error!({ error: 'Failed to release funds' }, 422)
          end
        end

        desc 'Initiate dispute'
        params do
          requires :id, type: String, desc: 'Trade ID'
          requires :dispute_reason, type: String, desc: 'Reason for dispute'
        end
        put ':id/dispute' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Check if the user is authorized to dispute this trade
          unless trade.can_be_disputed_by?(current_user)
            error!({ error: 'You are not authorized to dispute this trade' }, 403)
          end

          # Set dispute reason and mark as disputed
          trade.dispute_reason_param = params[:dispute_reason]

          # Create a trade service
          service = TradeService.new(trade)

          # Dispute the trade
          if service.dispute_trade!(params[:dispute_reason])
            present trade, with: V1::Trades::TradeDetail
          else
            error!({ error: 'Failed to initiate dispute' }, 422)
          end
        end

        desc 'Cancel trade'
        params do
          requires :id, type: String, desc: 'Trade ID'
          requires :cancel_reason, type: String, desc: 'Reason for cancellation'
        end
        put ':id/cancel' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Check if the user can cancel this trade
          unless trade.can_be_cancelled_by?(current_user)
            error!({ error: 'You are not authorized to cancel this trade' }, 403)
          end

          # Create a trade service
          service = TradeService.new(trade)

          # Cancel the trade
          if service.cancel_trade!(params[:cancel_reason])
            present trade, with: V1::Trades::TradeDetail
          else
            error!({ error: 'Failed to cancel trade' }, 422)
          end
        end

        desc 'Add a message to a trade'
        params do
          requires :id, type: String, desc: 'Trade ID'
          requires :body, type: String, desc: 'Message body'
        end
        post ':id/messages' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])
          error!({ error: 'Trade not found' }, 404) unless trade

          # Create the message
          message = trade.messages.new(
            user_id: current_user.id,
            body: params[:body],
            is_system: false
          )

          if message.save
            status 201  # Set status code to 201 Created for successful message creation
            present trade, with: V1::Trades::TradeDetailWithMessages
          else
            error!({ error: message.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Get messages for a trade'
        params do
          requires :id, type: String, desc: 'Trade ID'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 20, desc: 'Messages per page'
        end
        get ':id/messages' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])

          error!({ error: 'Trade not found' }, 404) unless trade

          # Get messages with pagination
          messages = trade.messages.order(created_at: :desc)
                          .page(params[:page]).per(params[:per_page])

          present messages, with: V1::Messages::Entity
        end

        desc 'Complete a trade (seller release)'
        params do
          requires :id, type: String, desc: 'Trade ID'
        end
        post ':id/complete' do
          trade = Trade.where('buyer_id = ? OR seller_id = ?', current_user.id, current_user.id).find_by(id: params[:id])
          error!({ error: 'Trade not found' }, 404) unless trade

          # Only seller can complete
          error!({ error: 'Only the seller can complete a trade' }, 403) unless current_user.id == trade.seller_id

          # Check if trade can be completed
          unless trade.may_complete?
            error!({ error: 'This trade cannot be completed in its current state' }, 400)
          end

          # Use TradeService to ensure Kafka events are sent
          service = TradeService.new(trade)
          if service.release_trade!
            # Create system message
            trade.messages.create!(
              user_id: current_user.id,
              body: 'Trade completed by seller. Funds released to buyer.',
              is_system: true
            )

            present trade, with: V1::Trades::TradeDetailWithMessages
          else
            error!({ error: 'Failed to complete trade' }, 422)
          end
        end

        # Additional endpoints for fiat deposits, withdrawals, etc. would be defined here
      end
    end
  end
end
