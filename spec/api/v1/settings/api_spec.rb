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
          'usdt_to_vnd' => 24500.0,
          'usdt_to_php' => 56.5,
          'usdt_to_ngn' => 1550.75
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
          'usdt_to_vnd' => 24500.0,
          'usdt_to_php' => 56.5,
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
          'usdt_erc20' => 25.5,
          'usdt_bep20' => 10.5,
          'usdt_solana' => 5.2,
          'usdt_trc20' => 1.8
        }
      )
    end

    it 'returns default withdrawal fees when no settings are explicitly set' do
      Setting.delete_all # Clean up existing settings

      get '/api/v1/settings/withdrawal_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'withdrawal_fees' => {
          'usdt_erc20' => 10.0,
          'usdt_bep20' => 1.0,
          'usdt_solana' => 3.0,
          'usdt_trc20' => 2.0
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
          'usdt_erc20' => 25.5,
          'usdt_bep20' => 10.5,
          'usdt_solana' => 3.0,
          'usdt_trc20' => 2.0
        }
      )
    end
  end

  describe 'GET /api/v1/settings/trading_fees' do
    it 'returns custom trading fees when set' do
      Setting.delete_all # Clean up existing settings

      Setting.vnd_trading_fee_ratio = '0.0025'
      Setting.php_trading_fee_ratio = '0.003'
      Setting.ngn_trading_fee_ratio = '0.004'
      Setting.default_trading_fee_ratio = '0.002'
      Setting.vnd_fixed_trading_fee = '1000'
      Setting.php_fixed_trading_fee = '50'
      Setting.ngn_fixed_trading_fee = '200'
      Setting.default_fixed_trading_fee = '500'

      get '/api/v1/settings/trading_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'trading_fees' => {
          'fee_ratios' => {
            'vnd' => 0.0025,
            'php' => 0.003,
            'ngn' => 0.004,
            'default' => 0.002
          },
          'fixed_fees' => {
            'vnd' => 1000.0,
            'php' => 50.0,
            'ngn' => 200.0,
            'default' => 500.0
          }
        }
      )
    end

    it 'returns default trading fees when no settings are explicitly set' do
      Setting.delete_all # Clean up existing settings

      get '/api/v1/settings/trading_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'trading_fees' => {
          'fee_ratios' => {
            'vnd' => 0.001,
            'php' => 0.001,
            'ngn' => 0.001,
            'default' => 0.001
          },
          'fixed_fees' => {
            'vnd' => 5000.0,
            'php' => 10.0,
            'ngn' => 300.0,
            'default' => 0.0
          }
        }
      )
    end

    it 'returns mix of custom and default trading fees' do
      Setting.delete_all # Clean up existing settings

      Setting.vnd_trading_fee_ratio = '0.0025'
      Setting.php_trading_fee_ratio = '0.003'
      Setting.vnd_fixed_trading_fee = '1000'
      Setting.php_fixed_trading_fee = '50'

      get '/api/v1/settings/trading_fees'

      expect(response).to have_http_status(:ok)
      expect(json_response).to eq(
        'trading_fees' => {
          'fee_ratios' => {
            'vnd' => 0.0025,
            'php' => 0.003,
            'ngn' => 0.001,
            'default' => 0.001
          },
          'fixed_fees' => {
            'vnd' => 1000.0,
            'php' => 50.0,
            'ngn' => 300.0,
            'default' => 0.0
          }
        }
      )
    end
  end
end
