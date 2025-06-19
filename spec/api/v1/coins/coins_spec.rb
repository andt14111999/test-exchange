# frozen_string_literal: true

require 'rails_helper'

describe 'Coins API', type: :request do
  describe 'GET /api/v1/coins' do
    before do
      allow(CoinConfig).to receive(:fiats).and_return(%w[vnd php ngn])
    end

    it 'returns the list of supported coins and fiats' do
      get '/api/v1/coins'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      expect(json_response['coins']).to eq(%w[usdt])
      expect(json_response['fiats']).to eq(%w[vnd php ngn])
    end
  end

  describe 'GET /api/v1/coin_settings' do
    before do
      CoinSetting.destroy_all
      @setting1 = CoinSetting.create!(currency: 'usdt', deposit_enabled: true, withdraw_enabled: true, swap_enabled: true, layers: [ { 'layer' => 'erc20', 'deposit_enabled' => true, 'withdraw_enabled' => true, 'swap_enabled' => true, 'maintenance' => false } ])
      @setting2 = CoinSetting.create!(currency: 'btc', deposit_enabled: false, withdraw_enabled: false, swap_enabled: false, layers: [ { 'layer' => 'native', 'deposit_enabled' => false, 'withdraw_enabled' => false, 'swap_enabled' => false, 'maintenance' => true } ])
    end

    it 'returns all coin settings with correct fields' do
      get '/api/v1/coin_settings'
      expect(response).to have_http_status(:ok)
      json = JSON.parse(response.body)
      expect(json).to be_an(Array)
      expect(json.size).to eq(2)
      expect(json[0].keys).to include('id', 'currency', 'deposit_enabled', 'withdraw_enabled', 'swap_enabled', 'layers', 'created_at', 'updated_at')
      expect(json[0]['currency']).to eq('usdt')
      expect(json[1]['currency']).to eq('btc')
      expect(json[0]['layers']).to be_an(Array)
      expect(json[0]['layers'][0]['layer']).to eq('erc20')
      expect(json[1]['layers'][0]['layer']).to eq('native')
    end
  end
end
