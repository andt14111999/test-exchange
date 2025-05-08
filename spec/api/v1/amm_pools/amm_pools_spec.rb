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
        # Không còn kiểm tra status vì đã xóa khỏi entity
        expect(pool['id']).to be_present
        expect(pool['pair']).to be_present
        expect(pool['token0']).to be_present
        expect(pool['token1']).to be_present
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

  describe 'GET /api/v1/amm_pools/active' do
    before do
      # Allow Kafka service to receive messages but not send them
      allow_any_instance_of(KafkaService::Services::AmmPool::AmmPoolService).to receive(:create)
      # Tắt các callback khi create để tests không bị fail
      allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)

      # Tạo các pool với status khác nhau và các pair/token khác nhau
      create(:amm_pool, status: 'active', pair: 'BTC/USDT', token0: 'BTC', token1: 'USDT')
      create(:amm_pool, status: 'active', pair: 'ETH/USDT', token0: 'ETH', token1: 'USDT')
      create(:amm_pool, status: 'active', pair: 'SOL/USDT', token0: 'SOL', token1: 'USDT')
      create(:amm_pool, status: 'pending', pair: 'DOGE/USDT', token0: 'DOGE', token1: 'USDT')
      create(:amm_pool, status: 'inactive', pair: 'ADA/USDT', token0: 'ADA', token1: 'USDT')
    end

    it 'returns only active pools with minimal information' do
      get '/api/v1/amm_pools/active'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      expect(json_response).to have_key('pools')
      expect(json_response['pools'].size).to eq(3)

      # Kiểm tra dữ liệu trả về có đúng định dạng không
      json_response['pools'].each do |pool|
        # Chỉ nên có các trường cơ bản
        expect(pool).to have_key('pair')
        expect(pool).to have_key('token0')
        expect(pool).to have_key('token1')

        # Không nên có các trường khác
        expect(pool).not_to have_key('status')
        expect(pool).not_to have_key('fee_percentage')
        expect(pool).not_to have_key('price')
      end
    end

    it 'does not include inactive or pending pools' do
      get '/api/v1/amm_pools/active'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      # Kiểm tra không có pair của pool inactive hoặc pending
      pairs = json_response['pools'].map { |pool| pool['pair'] }
      expect(pairs).not_to include('DOGE/USDT') # pending
      expect(pairs).not_to include('ADA/USDT') # inactive

      # Chỉ có pair của pool active
      expect(pairs).to contain_exactly('BTC/USDT', 'ETH/USDT', 'SOL/USDT')
    end

    it 'returns empty array when no active pools exist' do
      # Xóa tất cả pool
      AmmPool.destroy_all

      get '/api/v1/amm_pools/active'

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)

      expect(json_response['pools']).to be_empty
    end
  end

  describe 'GET /api/v1/amm_pools/:pair' do
    let(:amm_pool) do
      # Allow Kafka service to receive messages but not send them
      allow_any_instance_of(KafkaService::Services::AmmPool::AmmPoolService).to receive(:create)
      # Tắt các callback khi create để tests không bị fail
      allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)
      create(:amm_pool,
             status: 'active',
             price: 25000,
             current_tick: 100,
             total_value_locked_token0: 5000,
             total_value_locked_token1: 125000000,
             fee_growth_global0: 10)
    end

    it 'returns a specific amm pool with all fields' do
      get "/api/v1/amm_pools/#{CGI.escape(amm_pool.pair)}"

      expect(response).to have_http_status(:ok)

      json_response = JSON.parse(response.body)
      expect(json_response['id']).to eq(amm_pool.id)
      expect(json_response['pair']).to eq(amm_pool.pair)
      expect(json_response['token0']).to eq(amm_pool.token0)
      expect(json_response['token1']).to eq(amm_pool.token1)
      expect(json_response['tick_spacing']).to eq(amm_pool.tick_spacing)

      # Chuyển đổi giá trị để so sánh
      expect(json_response['fee_percentage'].to_f).to eq(amm_pool.fee_percentage)
      expect(json_response['current_tick']).to eq(amm_pool.current_tick)
      expect(json_response['price']).to eq(amm_pool.price.to_s)
      expect(json_response['sqrt_price']).to eq(amm_pool.sqrt_price.to_s)

      # Check APR and TVL fields
      expect(json_response['apr'].to_f).to eq(amm_pool.apr)
      expect(json_response['tvl_in_token0'].to_f).to eq(amm_pool.tvl_in_token0)
      expect(json_response['tvl_in_token1'].to_f).to eq(amm_pool.tvl_in_token1)

      # Check timestamps are integers
      expect(json_response['created_at']).to be_an(Integer)
      expect(json_response['updated_at']).to be_an(Integer)
    end

    it 'returns 404 for non-existent pool' do
      get '/api/v1/amm_pools/NONEXISTENT%2FPAIR'

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response).to have_key('error')
      expect(json_response['error']).to eq('AMM Pool not found')
    end

    it 'returns 404 for inactive pool' do
      inactive_pool = create(:amm_pool, status: 'inactive')
      allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)

      get "/api/v1/amm_pools/#{CGI.escape(inactive_pool.pair)}"

      expect(response).to have_http_status(:not_found)
      expect(JSON.parse(response.body)['error']).to eq('AMM Pool not found')
    end
  end
end
