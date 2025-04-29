# frozen_string_literal: true

module V1
  module Offers
    class Entity < Grape::Entity
      expose :id
      expose :user_id
      expose :merchant_display_name do |offer|
        offer.user.display_name
      end
      expose :offer_type
      expose :coin_currency
      expose :currency
      expose :price
      expose :min_amount
      expose :max_amount
      expose :total_amount
      expose :available_amount
      expose :payment_time
      expose :country_code
      expose :status do |offer|
        if offer.deleted?
          'deleted'
        elsif offer.disabled?
          'disabled'
        elsif offer.scheduled?
          offer.currently_active? ? 'scheduled_active' : 'scheduled_inactive'
        else
          'active'
        end
      end
      expose :online
      expose :automatic
      expose :created_at
    end

    class OfferDetail < Entity
      expose :user_id
      expose :payment_method_id
      expose :payment_details
      expose :terms_of_trade
      expose :disable_reason
      expose :margin
      expose :fixed_coin_price
      expose :bank_names
      expose :schedule_start_time
      expose :schedule_end_time
      expose :updated_at
    end
  end
end
