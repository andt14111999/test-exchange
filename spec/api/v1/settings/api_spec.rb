# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Settings::Api, type: :request do
  describe 'GET /api/v1/settings/exchange_rates' do
    it 'returns custom exchange rates when set' do
      Setting.delete_all # Clean up existing settings

      Setting.usdt_to_vnd_rate = '24500'
      Setting.usdt_to_php_rate = '56.5'
      Setting.usdt_to_ngn_rate = '1550.75'

      get '/api/v1/settings/exchange_rates'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'exchange_rates' => {
          'usdt_to_vnd' => '24500',
          'usdt_to_php' => '56.5',
          'usdt_to_ngn' => '1550.75'
        }
      )
    end

    it 'returns default rates when no settings are explicitly set' do
      Setting.delete_all # Clean up existing settings

      get '/api/v1/settings/exchange_rates'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'exchange_rates' => {
          'usdt_to_vnd' => 25000.0,
          'usdt_to_php' => 57.0,
          'usdt_to_ngn' => 450.0
        }
      )
    end

    it 'returns mix of custom and default rates' do
      Setting.delete_all # Clean up existing settings

      Setting.usdt_to_vnd_rate = '24500'
      Setting.usdt_to_php_rate = '56.5'

      get '/api/v1/settings/exchange_rates'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'exchange_rates' => {
          'usdt_to_vnd' => '24500',
          'usdt_to_php' => '56.5',
          'usdt_to_ngn' => 450.0 # Default value
        }
      )
    end
  end
end
