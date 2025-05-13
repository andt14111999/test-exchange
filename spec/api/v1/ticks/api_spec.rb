# frozen_string_literal: true

require 'rails_helper'

describe V1::Ticks::Api, type: :request do
  describe 'GET /api/v1/ticks' do
    it 'returns only active ticks grouped by pool pair' do
      amm_pool_first = create(:amm_pool, pair: 'USDT/VND', status: 'active')
      amm_pool_second = create(:amm_pool, pair: 'BTC/USDT', status: 'active')

      create(:tick, :active, amm_pool: amm_pool_first, pool_pair: amm_pool_first.pair, tick_index: 100)
      create(:tick, :active, amm_pool: amm_pool_first, pool_pair: amm_pool_first.pair, tick_index: 200)
      create(:tick, :inactive, amm_pool: amm_pool_first, pool_pair: amm_pool_first.pair, tick_index: 300)
      create(:tick, :active, amm_pool: amm_pool_second, pool_pair: amm_pool_second.pair, tick_index: 100)

      get '/api/v1/ticks'

      expect(response).to have_http_status(:ok)
      json = JSON.parse(response.body)

      # Should have two pool pairs
      expect(json.keys).to include(amm_pool_first.pair, amm_pool_second.pair)

      # First pool should have 2 active ticks
      expect(json[amm_pool_first.pair]['ticks'].size).to eq(2)
      expect(json[amm_pool_first.pair]['ticks'].map { |t| t['tick_index'] }).to include(100, 200)
      expect(json[amm_pool_first.pair]['ticks'].map { |t| t['tick_index'] }).not_to include(300)

      # Second pool should have 1 active tick
      expect(json[amm_pool_second.pair]['ticks'].size).to eq(1)
      expect(json[amm_pool_second.pair]['ticks'][0]['tick_index']).to eq(100)
    end
  end
end
