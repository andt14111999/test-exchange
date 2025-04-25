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

  describe 'GET /api/v1/settings/withdrawal_fees' do
    it 'returns custom withdrawal fees when set' do
      Setting.delete_all # Clean up existing settings

      Setting.usdt_erc20_withdrawal_fee = '25.5'
      Setting.usdt_bep20_withdrawal_fee = '10.5'
      Setting.usdt_solana_withdrawal_fee = '5.2'
      Setting.usdt_trc20_withdrawal_fee = '1.8'

      get '/api/v1/settings/withdrawal_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'withdrawal_fees' => {
          'usdt_erc20' => '25.5',
          'usdt_bep20' => '10.5',
          'usdt_solana' => '5.2',
          'usdt_trc20' => '1.8'
        }
      )
    end

    it 'returns default withdrawal fees when no settings are explicitly set' do
      Setting.delete_all # Clean up existing settings

      get '/api/v1/settings/withdrawal_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'withdrawal_fees' => {
          'usdt_erc20' => 10,
          'usdt_bep20' => 1,
          'usdt_solana' => 3,
          'usdt_trc20' => 2
        }
      )
    end

    it 'returns mix of custom and default withdrawal fees' do
      Setting.delete_all # Clean up existing settings

      Setting.usdt_erc20_withdrawal_fee = '25.5'
      Setting.usdt_bep20_withdrawal_fee = '10.5'

      get '/api/v1/settings/withdrawal_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'withdrawal_fees' => {
          'usdt_erc20' => '25.5',
          'usdt_bep20' => '10.5',
          'usdt_solana' => 3,
          'usdt_trc20' => 2
        }
      )
    end
  end
end
