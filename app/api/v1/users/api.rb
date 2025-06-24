module V1
  module Users
    class Api < Grape::API
      helpers V1::Helpers::AuthHelper
      helpers V1::Helpers::DeviceHelper

      before { authenticate_user! }

      resource :users do
        desc 'Get current user information'
        get :me do
          present current_user, with: V1::Users::Entity
        end

        desc 'Update username'
        params do
          requires :username, type: String, desc: 'New username'
        end
        patch :username do
          if current_user.update(username: params[:username])
            present current_user, with: V1::Users::Entity
          else
            error!({ errors: current_user.errors.full_messages }, 422)
          end
        end

        # 2FA endpoints
        namespace :two_factor_auth do
          desc 'Get 2FA status'
          get :status do
            { enabled: current_user.authenticator_enabled }
          end

          desc 'Enable 2FA - Generate QR code'
          post :enable do
            if current_user.authenticator_enabled
              error!({ message: '2FA is already enabled' }, 400)
            end

            current_user.assign_authenticator_key
            if current_user.save
              status 200
              {
                qr_code_uri: current_user.generate_provisioning_uri,
                message: 'Scan the QR code with your authenticator app, then verify with a code'
              }
            else
              error!({ errors: current_user.errors.full_messages }, 422)
            end
          end

          desc 'Verify and activate 2FA'
          params do
            requires :code, type: String, desc: '6-digit verification code from authenticator app'
          end
          post :verify do
            if current_user.authenticator_enabled
              error!({ message: '2FA is already enabled' }, 400)
            end

            if current_user.authenticator_key.blank?
              error!({ message: 'Please enable 2FA first' }, 400)
            end

            if current_user.verify_otp(params[:code])
              current_user.update!(authenticator_enabled: true)
              status 200
              { message: '2FA has been successfully enabled' }
            else
              error!({ message: 'Invalid verification code' }, 400)
            end
          end

          desc 'Disable 2FA'
          params do
            requires :code, type: String, desc: '6-digit verification code from authenticator app'
          end
          delete :disable do
            unless current_user.authenticator_enabled
              error!({ message: '2FA is not enabled' }, 400)
            end

            if current_user.verify_otp(params[:code])
              current_user.disable_authenticator!
              current_user.save!
              { message: '2FA has been successfully disabled' }
            else
              error!({ message: 'Invalid verification code' }, 400)
            end
          end
        end

        # Access Devices Management
        namespace :access_devices do
          desc 'Get list of trusted devices'
          get do
            devices = current_user.access_devices.order(created_at: :desc)
            present devices, with: V1::Users::AccessDeviceEntity
          end

          desc 'Get current device info'
          get :current do
            device = create_or_find_access_device
            if device
              present device, with: V1::Users::AccessDeviceEntity
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

          desc 'Mark current device as trusted'
          post :trust do
            device = create_or_find_access_device
            if device
              device.update(first_device: true)
              present device, with: V1::Users::AccessDeviceEntity
            else
              error!({ message: 'Device UUID header missing' }, 400)
            end
          end
        end
      end
    end
  end
end
