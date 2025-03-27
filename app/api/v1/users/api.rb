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
      end
    end
  end
end
