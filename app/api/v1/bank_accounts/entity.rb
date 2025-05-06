# frozen_string_literal: true

module V1
  module BankAccounts
    class Entity < Grape::Entity
      expose :id
      expose :bank_name
      expose :account_name
      expose :account_number
      expose :branch
      expose :country_code
      expose :verified
      expose :is_primary
      expose :created_at
    end

    class DetailEntity < Entity
      expose :user_id
      expose :updated_at
    end
  end
end
