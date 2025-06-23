module V1
  module Users
    class Api < Grape::API
      helpers V1::Helpers::AuthHelper

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
      end
    end
  end
end
