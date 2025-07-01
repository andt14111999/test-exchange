# frozen_string_literal: true

module V1
  module Banks
    class Api < Grape::API
      resource :banks do
        desc 'Get banks by country code'
        params do
          requires :country_code, type: String, desc: 'Country code (VN, NG, GH)'
        end
        get do
          banks = Bank.by_country(params[:country_code]).includes(:country).ordered

          present :status, 'success'
          present :data, banks, with: V1::Banks::Entity
        end
      end
    end
  end
end
