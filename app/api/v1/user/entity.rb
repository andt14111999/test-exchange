# frozen_string_literal: true

module V1
  module User
    class Entity < Grape::Entity
      expose :id
      expose :email
      expose :display_name
      expose :avatar_url
      expose :role
      expose :status
      expose :kyc_level
      expose :phone_verified
      expose :document_verified
    end
  end
end
