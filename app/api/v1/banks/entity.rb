# frozen_string_literal: true

module V1
  module Banks
    class Entity < Grape::Entity
      expose :id
      expose :name
      expose :code
      expose :bin
      expose :short_name, as: :shortName
      expose :logo
      expose :transfer_supported, as: :transferSupported
      expose :lookup_supported, as: :lookupSupported
      expose :support
      expose :is_transfer, as: :isTransfer
      expose :swift_code, as: :swiftCode
      expose :country_code do |bank|
        bank.country&.code
      end
      expose :country_name do |bank|
        bank.country&.name
      end
    end
  end
end
