# frozen_string_literal: true

module V1
  module Merchant
    class FiatTransactionEntity < Grape::Entity
      expose :id
      expose :amount
      expose :transaction_type
      expose :currency
      expose :created_at
      expose :updated_at
    end
  end
end
