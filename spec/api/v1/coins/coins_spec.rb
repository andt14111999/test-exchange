# frozen_string_literal: true

require 'rails_helper'

describe V1::Coins::Api, type: :request do
  describe 'GET /api/v1/coins' do
    before do
      allow(CoinConfig).to receive(:coins).and_return(%w[usdt btc eth])
      allow(CoinConfig).to receive(:fiats).and_return(%w[vnd php ngn])
    end

    it 'returns the list of supported coins and fiats' do
      get '/api/v1/coins'
      
      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      
      expect(json_response['coins']).to eq(%w[usdt btc eth])
      expect(json_response['fiats']).to eq(%w[vnd php ngn])
    end
  end
end
