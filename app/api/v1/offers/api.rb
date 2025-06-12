# frozen_string_literal: true

require_relative 'entity'

module V1
  module Offers
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :offers do
        desc 'Get all active offers'
        params do
          optional :offer_type, type: String, values: Offer::OFFER_TYPES, desc: 'Filter by offer type (buy/sell)'
          optional :coin_currency, type: String, desc: 'Filter by coin currency (e.g., btc, usdt)'
          optional :currency, type: String, desc: 'Filter by fiat currency (e.g., VND, PHP)'
          optional :country_code, type: String, desc: 'Filter by country code'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 20, desc: 'Items per page'
          optional :sort, type: String, values: %w[price_asc price_desc newest oldest], default: 'price_asc', desc: 'Sort order'
        end
        get do
          offers = Offer.where(deleted: false)

          # Apply filters
          offers = offers.buy_offers if params[:offer_type] == 'buy'
          offers = offers.sell_offers if params[:offer_type] == 'sell'
          offers = offers.of_coin_currency(params[:coin_currency]) if params[:coin_currency].present?
          offers = offers.of_currency(params[:currency]) if params[:currency].present?
          offers = offers.of_country(params[:country_code]) if params[:country_code].present?

          # Apply sorting
          offers = case params[:sort]
          when 'price_asc' then offers.price_asc
          when 'price_desc' then offers.price_desc
          when 'newest' then offers.newest_first
          when 'oldest' then offers.oldest_first
          else offers.price_asc
          end

          offers = offers.page(params[:page]).per(params[:per_page])

          present offers, with: V1::Offers::Entity
        end

        desc 'Get all merchant offers including disabled ones'
        get 'merchant' do
          # Get all offers for current merchant except deleted ones
          offers = current_user.offers.where(deleted: false)

          # Apply sorting - newest first by default
          offers = offers.newest_first

          present offers, with: V1::Offers::Entity
        end

        desc 'Get offer details'
        params do
          requires :id, type: String, desc: 'Offer ID'
        end
        get ':id' do
          offer = Offer.find(params[:id])
          present offer, with: V1::Offers::OfferDetail
        end

        desc 'Create new offer'
        params do
          requires :offer_type, type: String, values: Offer::OFFER_TYPES, desc: 'Offer type (buy/sell)'
          requires :coin_currency, type: String, desc: 'Coin currency (e.g., btc, usdt)'
          requires :currency, type: String, desc: 'Fiat currency (e.g., VND, PHP)'
          requires :price, type: BigDecimal, desc: 'Fixed price per coin'
          optional :margin, type: BigDecimal, desc: 'Margin percentage instead of fixed price'
          requires :min_amount, type: BigDecimal, desc: 'Minimum trade amount'
          requires :max_amount, type: BigDecimal, desc: 'Maximum trade amount'
          requires :total_amount, type: BigDecimal, desc: 'Total amount available'
          requires :payment_method_id, type: Integer, desc: 'Payment method ID'
          requires :payment_time, type: Integer, desc: 'Payment time limit in minutes'
          requires :payment_details, type: Hash, desc: 'Payment details'
          requires :country_code, type: String, desc: 'Country code'
          optional :terms_of_trade, type: String, desc: 'Terms of trade'
          optional :bank_names, type: Array, desc: 'Accepted bank names'
          optional :schedule_start_time, type: DateTime, desc: 'Schedule start time'
          optional :schedule_end_time, type: DateTime, desc: 'Schedule end time'
          optional :automatic, type: Boolean, default: false, desc: 'Is automatic'
        end
        post do
          # Check for user limits
          active_offers_count = current_user.offers.currently_active.count
          max_offers = current_user.max_active_offers || 5

          if active_offers_count >= max_offers
            error!({ error: "You can have at most #{max_offers} active offers" }, 400)
          end

          # Check for KYC/level requirements
          unless current_user.can_create_offer?
            error!({ error: 'Account not verified or insufficient level to create offers' }, 403)
          end

          # Validate amounts
          if params[:min_amount] > params[:max_amount]
            error!({ error: 'Minimum amount must be less than maximum amount' }, 400)
          end

          if params[:max_amount] > params[:total_amount]
            error!({ error: 'Maximum amount must be less than total amount' }, 400)
          end

          offer = current_user.offers.new(declared(params, include_missing: false))

          # Set default values
          offer.online = true

          # Use margin or fixed price
          if params[:margin].present?
            offer.margin = params[:margin]
            offer.update_price_from_market!
          end

          if offer.save
            present offer, with: V1::Offers::OfferDetail
          else
            error!({ error: offer.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Update offer'
        params do
          requires :id, type: String, desc: 'Offer ID'
          optional :price, type: BigDecimal, desc: 'Fixed price per coin'
          optional :margin, type: BigDecimal, desc: 'Margin percentage instead of fixed price'
          optional :min_amount, type: BigDecimal, desc: 'Minimum trade amount'
          optional :max_amount, type: BigDecimal, desc: 'Maximum trade amount'
          optional :total_amount, type: BigDecimal, desc: 'Total amount available'
          optional :payment_method_id, type: Integer, desc: 'Payment method ID'
          optional :payment_time, type: Integer, desc: 'Payment time limit in minutes'
          optional :payment_details, type: Hash, desc: 'Payment details'
          optional :terms_of_trade, type: String, desc: 'Terms of trade'
          optional :bank_names, type: Array, desc: 'Accepted bank names'
          optional :schedule_start_time, type: DateTime, desc: 'Schedule start time'
          optional :schedule_end_time, type: DateTime, desc: 'Schedule end time'
          optional :automatic, type: Boolean, desc: 'Is automatic'
        end
        put ':id' do
          offer = current_user.offers.with_deleted.find(params[:id])

          if offer.deleted?
            error!({ error: 'Cannot update a deleted offer' }, 400)
          end

          # Cannot change offer_type
          if params.key?(:offer_type)
            error!({ error: 'Cannot change offer type' }, 400)
          end

          # Check constraints
          if params[:min_amount].present? && params[:max_amount].present?
            if params[:min_amount] > params[:max_amount]
              error!({ error: 'Minimum amount must be less than maximum amount' }, 400)
            end
          elsif params[:min_amount].present? && params[:min_amount] > offer.max_amount
            error!({ error: 'Minimum amount must be less than maximum amount' }, 400)
          elsif params[:max_amount].present? && params[:max_amount] < offer.min_amount
            error!({ error: 'Maximum amount must be greater than minimum amount' }, 400)
          end

          if params[:max_amount].present? && params[:total_amount].present?
            if params[:max_amount] > params[:total_amount]
              error!({ error: 'Maximum amount must be less than total amount' }, 400)
            end
          elsif params[:max_amount].present? && params[:max_amount] > offer.total_amount
            error!({ error: 'Maximum amount must be less than total amount' }, 400)
          elsif params[:total_amount].present? && params[:total_amount] < offer.max_amount
            error!({ error: 'Total amount must be greater than maximum amount' }, 400)
          end

          # Use margin or fixed price
          if params[:margin].present?
            offer.margin = params[:margin]
            offer.update_price_from_market!
            params.delete(:price) # Ignore price if margin is provided
          end

          if offer.update(declared(params, include_missing: false))
            present offer, with: V1::Offers::OfferDetail
          else
            error!({ error: offer.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Disable offer'
        params do
          requires :id, type: String, desc: 'Offer ID'
        end
        put ':id/disable' do
          offer = current_user.offers.with_deleted.find(params[:id])

          if offer.deleted?
            error!({ error: 'Cannot disable a deleted offer' }, 400)
          end

          if offer.disabled?
            error!({ error: 'Offer is already disabled' }, 400)
          end

          if offer.disable!
            present offer, with: V1::Offers::OfferDetail
          else
            error!({ error: offer.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Enable offer'
        params do
          requires :id, type: String, desc: 'Offer ID'
        end
        put ':id/enable' do
          offer = current_user.offers.with_deleted.find(params[:id])

          if offer.deleted?
            error!({ error: 'Cannot enable a deleted offer' }, 400)
          end

          unless offer.disabled?
            error!({ error: 'Offer is not disabled' }, 400)
          end

          # Check if offer can be enabled
          if offer.available_amount < offer.min_amount
            error!({ error: 'Available amount is too low to enable offer' }, 400)
          end

          if offer.enable!
            present offer, with: V1::Offers::OfferDetail
          else
            error!({ error: offer.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Delete offer'
        params do
          requires :id, type: String, desc: 'Offer ID'
        end
        delete ':id' do
          offer = current_user.offers.with_deleted.find(params[:id])

          if offer.deleted?
            error!({ error: 'Offer is already deleted' }, 400)
          end

          if offer.delete!
            present offer, with: V1::Offers::OfferDetail
          else
            error!({ error: offer.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Set offer online/offline'
        params do
          requires :id, type: String, desc: 'Offer ID'
          requires :online, type: Boolean, desc: 'Online status'
        end
        put ':id/online_status' do
          offer = current_user.offers.with_deleted.find(params[:id])

          if offer.deleted?
            error!({ error: 'Cannot change online status for a deleted offer' }, 400)
          end

          if params[:online]
            result = offer.set_online!
          else
            result = offer.set_offline!
          end

          if result
            present offer, with: V1::Offers::OfferDetail
          else
            error!({ error: offer.errors.full_messages.join(', ') }, 422)
          end
        end
      end
    end
  end
end
