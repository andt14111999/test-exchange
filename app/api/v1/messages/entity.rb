# frozen_string_literal: true

module V1
  module Messages
    class Entity < Grape::Entity
      expose :id
      expose :user_id
      expose :body
      expose :is_system
      expose :created_at
      expose :updated_at
      expose :username do |message|
        message.user&.display_name
      end
    end
  end
end
