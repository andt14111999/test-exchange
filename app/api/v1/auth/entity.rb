# frozen_string_literal: true

module V1
  module Auth
    class Entity < Grape::Entity
      expose :token
      expose :user, using: V1::Users::Entity
    end
  end
end
