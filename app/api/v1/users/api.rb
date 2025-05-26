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
      end
    end
  end
end
