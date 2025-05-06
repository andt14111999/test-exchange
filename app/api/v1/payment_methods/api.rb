module V1
  module PaymentMethods
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :payment_methods do
        desc 'Get all payment methods'
        params do
          optional :country_code, type: String, desc: 'Filter by country code'
          optional :enabled, type: Boolean, desc: 'Filter by enabled status'
        end
        get do
          payment_methods = PaymentMethod.all

          # Apply filters
          payment_methods = payment_methods.of_country(params[:country_code]) if params[:country_code].present?
          payment_methods = payment_methods.enabled if params[:enabled] == true
          payment_methods = payment_methods.disabled if params[:enabled] == false

          present payment_methods, with: V1::PaymentMethods::Entity
        end

        desc 'Get payment method details'
        params do
          requires :id, type: String, desc: 'Payment method ID'
        end
        get ':id' do
          payment_method = PaymentMethod.find(params[:id])
          present payment_method, with: V1::PaymentMethods::Entity
        end
      end
    end
  end
end
