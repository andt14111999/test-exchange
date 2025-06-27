# frozen_string_literal: true

module V1
  module AccessDevices
    class Entity < Grape::Entity
      expose :id
      expose :display_name
      expose :location
      expose :first_device
      expose :trusted
      expose :device_type
      expose :ip_address
      expose :created_at
      expose :updated_at
    end
  end
end
