# frozen_string_literal: true

module V1
  module FiatAccount
    class Entity < Grape::Entity
      expose :currency
      expose :currency_name
      expose :balance
      expose :frozen_balance
    end
  end
end
