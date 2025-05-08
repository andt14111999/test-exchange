# frozen_string_literal: true

module V1
  module AmmOrders
    class Entity < Grape::Entity
      format_with(:decimal) { |number| number.to_s }

      expose :id
      expose :identifier
      expose :zero_for_one
      expose :status
      expose :error_message
      expose :before_tick_index
      expose :after_tick_index

      expose :amount_specified, format_with: :decimal
      expose :amount_estimated, format_with: :decimal
      expose :amount_actual, format_with: :decimal
      expose :amount_received, format_with: :decimal
      expose :slippage, format_with: :decimal

      expose :fees

      expose :created_at do |order|
        order.created_at.to_i
      end

      expose :updated_at do |order|
        order.updated_at.to_i
      end
    end
  end
end
