# frozen_string_literal: true

module V1
  module Merchant
    class Entity < Grape::Entity
      expose :id
      expose :usdt_amount
      expose :fiat_amount
      expose :fiat_currency
      expose :status
    end
  end
end
