# frozen_string_literal: true

module V1
  module Merchant
    class EscrowEntity < Grape::Entity
      expose :id
      expose :usdt_amount
      expose :fiat_amount
      expose :fiat_currency
      expose :status
      expose :created_at
      expose :updated_at
    end
  end
end
