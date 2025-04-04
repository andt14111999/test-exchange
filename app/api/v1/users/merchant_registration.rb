# frozen_string_literal: true

module V1
  module Users
    class MerchantRegistration < Grape::API
      helpers V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :merchant_registration do
        desc 'Register as a merchant'
        post do
          service = MerchantRegistrationService.new(current_user)

          if service.call
            present current_user, with: V1::Users::Entity
          else
            error!({ status: 'error', message: 'Cannot register as merchant. Please ensure you have completed KYC level 2 and your account is active.' }, 422)
          end
        end
      end
    end
  end
end
