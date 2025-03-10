# frozen_string_literal: true

module V1
  module Auth
    class Entity < Grape::Entity
      expose :token
      expose :user, with: V1::User::Entity
    end
  end
end
