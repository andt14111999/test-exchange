# frozen_string_literal: true

module V1
  module Users
    class AccessDeviceEntity < Grape::Entity
      expose :id
      expose :display_name
      expose :location
      expose :first_device
      expose :trusted?, as: :trusted
      expose :device_type
      expose :ip_address
      expose :created_at
      expose :updated_at
    end
  end
end
