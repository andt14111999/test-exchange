# frozen_string_literal: true

module V1
  module Test
    class Entity < Grape::Entity
      expose :id
      expose :created_at
    end
  end
end
