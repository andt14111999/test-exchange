# frozen_string_literal: true

module V1
  module Settings
    class Api < Grape::API
      format :json

      resource :settings do
        desc 'Get exchange rates'
        get :exchange_rates do
          present_exchange_rates
        end
      end

      helpers do
        def present_exchange_rates
          {
            exchange_rates: {
              usdt_to_vnd: Setting.usdt_to_vnd_rate,
              usdt_to_php: Setting.usdt_to_php_rate,
              usdt_to_ngn: Setting.usdt_to_ngn_rate
            }
          }
        end
      end
    end
  end
end
