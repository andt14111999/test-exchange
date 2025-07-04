# frozen_string_literal: true

module V1
  module Users
    class Entity < Grape::Entity
      expose :id
      expose :email
      expose :username
      expose :display_name
      expose :avatar_url
      expose :role
      expose :status
      expose :kyc_level
      expose :phone_verified
      expose :document_verified
      expose :authenticator_enabled
    end
  end
end
