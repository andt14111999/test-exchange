# frozen_string_literal: true

module V1
  module Auth
    class Entity < Grape::Entity
      expose :token
    end
  end
end
