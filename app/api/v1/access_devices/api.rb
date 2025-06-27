# frozen_string_literal: true

module V1
  module AccessDevices
    class Api < Grape::API
      helpers V1::Helpers::AuthHelper
      helpers V1::Helpers::DeviceHelper

      before { authenticate_user! }

      resource :access_devices do
        desc 'Get list of trusted devices'
        get do
          devices = current_user.access_devices.order(created_at: :desc)
          present devices, with: V1::AccessDevices::Entity
        end

        desc 'Get current device info'
        get :current do
          device = create_or_find_access_device
          if device
            present device, with: V1::AccessDevices::Entity
          else
            error!({ message: 'Device UUID header missing' }, 400)
          end
        end

        desc 'Remove a trusted device'
        params do
          requires :id, type: Integer, desc: 'Device ID to remove'
        end
        delete ':id' do
          device = current_user.access_devices.find_by(id: params[:id])

          unless device
            error!({ message: 'Device not found' }, 404)
          end

          # Don't allow removing the current device if it's the only first device
          if device.first_device && current_user.access_devices.where(first_device: true).count == 1
            error!({ message: 'Cannot remove the only first device' }, 400)
          end

          device.destroy
          { message: 'Device removed successfully' }
        end
      end
    end
  end
end
