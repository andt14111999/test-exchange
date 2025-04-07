require 'rails_helper'

describe 'AmmPools API', type: :request do
  describe 'GET /api/v1/amm_pools' do
    before do
      # Allow Kafka service to receive messages but not send them
      allow_any_instance_of(KafkaService::Services::AmmPool::AmmPoolService).to receive(:create)
      # Tắt các callback khi create để tests không bị fail
      allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)
      create_list(:amm_pool, 2, status: 'active')
      create(:amm_pool, status: 'pending')
      create(:amm_pool, status: 'inactive')
    end

    it 'returns only active amm pools with pagination' do
      get '/api/v1/amm_pools'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      expect(json_response['amm_pools'].size).to eq(2)

      # Check that all returned pools are active
      json_response['amm_pools'].each do |pool|
        expect(pool['status']).to eq('active')
      end
    end

    it 'includes metadata with pagination info in the response' do
      get '/api/v1/amm_pools'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      expect(json_response).to have_key('meta')
      expect(json_response['meta']).to have_key('current_page')
      expect(json_response['meta']).to have_key('per_page')
      expect(json_response['meta']).to have_key('total_pages')
      expect(json_response['meta']['current_page']).to eq(1)
      expect(json_response['meta']['per_page']).to eq(10)
    end

    it 'handles custom pagination parameters' do
      get '/api/v1/amm_pools?page=2&per_page=1'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      expect(json_response['meta']['current_page']).to eq(2)
      expect(json_response['meta']['per_page']).to eq(1)
    end
  end

  describe 'GET /api/v1/amm_pools/:id' do
    let(:amm_pool) do
      # Allow Kafka service to receive messages but not send them
      allow_any_instance_of(KafkaService::Services::AmmPool::AmmPoolService).to receive(:create)
      # Tắt các callback khi create để tests không bị fail
      allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)
      create(:amm_pool,
             status: 'active',
             price: 25000,
             current_tick: 100,
             volume_token0: 1000,
             volume_token1: 25000000,
             volume_usd: 1000,
             tx_count: 50,
             total_value_locked_token0: 5000,
             total_value_locked_token1: 125000000,
             init_price: 24000)
    end

    it 'returns a specific amm pool with all fields' do
      get "/api/v1/amm_pools/#{amm_pool.id}"

      expect(response).to have_http_status(:ok)

      json_response = JSON.parse(response.body)
      expect(json_response['id']).to eq(amm_pool.id)
      expect(json_response['pair']).to eq(amm_pool.pair)
      expect(json_response['token0']).to eq(amm_pool.token0)
      expect(json_response['token1']).to eq(amm_pool.token1)
      expect(json_response['tick_spacing']).to eq(amm_pool.tick_spacing)

      # Chuyển đổi giá trị để so sánh
      expect(json_response['fee_percentage'].to_f).to eq(amm_pool.fee_percentage)
      expect(json_response['fee_protocol_percentage'].to_f).to eq(amm_pool.fee_protocol_percentage)
      expect(json_response['current_tick']).to eq(amm_pool.current_tick)
      expect(json_response['price']).to eq(amm_pool.price.to_s)
      expect(json_response['status']).to eq(amm_pool.status)
      expect(json_response['init_price']).to eq(amm_pool.init_price.to_s)

      # Check volume and TVL fields
      expect(json_response['volume_token0']).to eq(amm_pool.volume_token0.to_s)
      expect(json_response['volume_token1']).to eq(amm_pool.volume_token1.to_s)
      expect(json_response['volume_usd']).to eq(amm_pool.volume_usd.to_s)
      expect(json_response['tx_count']).to eq(amm_pool.tx_count)
      expect(json_response['total_value_locked_token0']).to eq(amm_pool.total_value_locked_token0.to_s)
      expect(json_response['total_value_locked_token1']).to eq(amm_pool.total_value_locked_token1.to_s)

      # Check timestamps are integers
      expect(json_response['created_at']).to be_an(Integer)
      expect(json_response['updated_at']).to be_an(Integer)
    end

    it 'returns 404 for non-existent pool' do
      get '/api/v1/amm_pools/999999'

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response).to have_key('error')
      expect(json_response['error']).to eq('AMM Pool not found')
    end

    it 'returns 404 for inactive pool' do
      inactive_pool = create(:amm_pool, status: 'inactive')
      allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)

      get "/api/v1/amm_pools/#{inactive_pool.id}"

      expect(response).to have_http_status(:not_found)
      expect(JSON.parse(response.body)['error']).to eq('AMM Pool not found')
    end
  end
end
