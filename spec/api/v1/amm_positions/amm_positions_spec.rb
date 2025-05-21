# frozen_string_literal: true

require 'rails_helper'

describe 'AmmPositions API', type: :request do
  let(:user) { create(:user) }
  let(:auth_token) { JsonWebToken.encode(user_id: user.id) }
  let(:headers) { { 'Authorization' => "Bearer #{auth_token}" } }
  let(:pool) { create(:amm_pool, pair: 'USDT/VND', token0: 'USDT', token1: 'VND') }

  before do
    # Thay vì mock authentication helper, tạo các account thực để test
    create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 1000, frozen_balance: 0)
    create(:fiat_account, user: user, currency: 'VND', balance: 1000, frozen_balance: 0)
  end

  describe 'GET /api/v1/amm_positions' do
    before do
      allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
      create_list(:amm_position, 3, user: user, status: 'open')
      create_list(:amm_position, 2, user: user, status: 'pending')
      create_list(:amm_position, 1, user: create(:user), status: 'open')
    end

    it 'returns positions for the current user with default status open' do
      get "/api/v1/amm_positions", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_positions'].size).to eq(3)
      expect(json_response['amm_positions'].all? { |p| p['status'] == 'open' }).to be true
    end

    it 'returns all positions when status is all' do
      get "/api/v1/amm_positions?status=all", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_positions'].size).to eq(5)
    end

    it 'filters positions by specific status' do
      get "/api/v1/amm_positions?status=pending", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_positions'].size).to eq(2)
      expect(json_response['amm_positions'].all? { |p| p['status'] == 'pending' }).to be true
    end

    it 'paginates results' do
      get "/api/v1/amm_positions?status=all&per_page=2&page=1", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_positions'].size).to eq(2)
      expect(json_response['amm_positions'].length).to be <= 2
    end
  end

  describe 'POST /api/v1/amm_positions' do
    let(:valid_params) do
      {
        pool_pair: 'USDT/VND',
        tick_lower_index: -100,
        tick_upper_index: 100,
        amount0_initial: 100,
        amount1_initial: 100
      }
    end

    before do
      allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
      allow(AmmPool).to receive(:find_by).with(pair: 'USDT/VND').and_return(pool)
      allow(pool).to receive(:tick_spacing).and_return(10)
    end

    it 'creates a new position' do
      expect {
        post '/api/v1/amm_positions',
          params: valid_params.merge(tick_lower_index: -100, tick_upper_index: 100),
          headers: headers
      }.to change(AmmPosition, :count).by(1)

      expect(response).to have_http_status(:created)
      json_response = JSON.parse(response.body)
      expect(json_response['pool_pair']).to eq('USDT/VND')
      expect(json_response['status']).to eq('pending')
    end

    it 'returns 404 if pool is not found' do
      allow(AmmPool).to receive(:find_by).with(pair: 'USDT/VND').and_return(nil)

      post '/api/v1/amm_positions', params: valid_params, headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Pool not found')
    end

    it 'returns validation errors for invalid params' do
      invalid_params = valid_params.merge(amount0_initial: -1)

      post '/api/v1/amm_positions', params: invalid_params, headers: headers

      expect(response).to have_http_status(:unprocessable_entity)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to include('must be greater than or equal to 0')
    end

    it 'generates an identifier for the position' do
      allow(Time).to receive(:now).and_return(Time.at(1650000000))

      post '/api/v1/amm_positions', params: valid_params, headers: headers

      json_response = JSON.parse(response.body)
      expected_identifier = "amm_position_#{user.id}_usdt/vnd_1650000000"
      expect(json_response['identifier']).to eq(expected_identifier)
    end

    it 'returns validation error when tick indices validation fails' do
      post '/api/v1/amm_positions',
        params: valid_params.merge(tick_lower_index: 100, tick_upper_index: 50),
        headers: headers

      expect(response).to have_http_status(:unprocessable_entity)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to include('Tick lower index must be less than')
    end

    it 'returns validation error when account balance is insufficient' do
      # Cập nhật account để có balance không đủ
      user.coin_accounts.find_by(coin_currency: 'usdt').update(balance: 50)

      post '/api/v1/amm_positions', params: valid_params, headers: headers

      expect(response).to have_http_status(:unprocessable_entity)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to include('exceeds available balance')
    end
  end

  describe 'GET /api/v1/amm_positions/:id' do
    let!(:position) { create(:amm_position, user: user, identifier: 'test_position_123') }
    let!(:open_position) { create(:amm_position, user: user, identifier: 'open_position_123', status: 'open') }
    let!(:other_position) { create(:amm_position, identifier: 'other_position_123') }

    before do
      allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
      allow_any_instance_of(AmmPosition).to receive(:calculate_est_fee)
    end

    it 'returns the position with the specified id' do
      get "/api/v1/amm_positions/#{position.id}", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['id']).to eq(position.id)

      # Check that the detail fields are included
      expect(json_response).to have_key('amount0_withdrawal')
      expect(json_response).to have_key('amount1_withdrawal')
      expect(json_response).to have_key('estimate_fee_token0')
      expect(json_response).to have_key('estimate_fee_token1')
      expect(json_response).to have_key('apr')
    end

    it 'calculates estimated fees for open positions' do
      expect_any_instance_of(AmmPosition).to receive(:calculate_est_fee)

      get "/api/v1/amm_positions/#{open_position.id}", headers: headers

      expect(response).to have_http_status(:ok)
    end

    it 'does not calculate fees for non-open positions' do
      expect(position).not_to receive(:calculate_est_fee)

      get "/api/v1/amm_positions/#{position.id}", headers: headers

      expect(response).to have_http_status(:ok)
    end

    it 'returns 404 if position is not found for the current user' do
      get "/api/v1/amm_positions/#{other_position.id}", headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Position not found')
    end

    it 'returns 404 if position does not exist' do
      get '/api/v1/amm_positions/999999', headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Position not found')
    end
  end

  describe 'POST /api/v1/amm_positions/:id/collect_fee' do
    let!(:position) { create(:amm_position, user: user, identifier: 'test_position_123', status: 'open') }
    let!(:pending_position) { create(:amm_position, user: user, identifier: 'pending_position', status: 'pending') }
    let!(:other_position) { create(:amm_position, identifier: 'other_position_123', status: 'open') }

    before do
      allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
      allow_any_instance_of(AmmPosition).to receive(:collect_fee)
    end

    it 'initiates fee collection for the position' do
      post "/api/v1/amm_positions/#{position.id}/collect_fee", headers: headers

      expect(response).to have_http_status(:created)
      json_response = JSON.parse(response.body)
      expect(json_response['success']).to be true
      expect(json_response['message']).to eq('Fee collection initiated')
    end

    it 'returns 404 if position is not found for the current user' do
      post "/api/v1/amm_positions/#{other_position.id}/collect_fee", headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Position not found')
    end

    it 'returns 422 if position is not in open state' do
      post "/api/v1/amm_positions/#{pending_position.id}/collect_fee", headers: headers

      expect(response).to have_http_status(:unprocessable_entity)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Cannot collect fee for a position that is not open')
    end
  end

  describe 'POST /api/v1/amm_positions/:id/close' do
    let!(:position) { create(:amm_position, user: user, identifier: 'test_position_123', status: 'open') }
    let!(:pending_position) { create(:amm_position, user: user, identifier: 'pending_position', status: 'pending') }
    let!(:other_position) { create(:amm_position, identifier: 'other_position_123', status: 'open') }

    before do
      allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
      allow_any_instance_of(AmmPosition).to receive(:close_position)
    end

    it 'initiates position closing' do
      post "/api/v1/amm_positions/#{position.id}/close", headers: headers

      expect(response).to have_http_status(:created)
      json_response = JSON.parse(response.body)
      expect(json_response['success']).to be true
      expect(json_response['message']).to eq('Position closing initiated')
    end

    it 'returns 404 if position is not found for the current user' do
      post "/api/v1/amm_positions/#{other_position.id}/close", headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Position not found')
    end

    it 'returns 422 if position is not in open state' do
      post "/api/v1/amm_positions/#{pending_position.id}/close", headers: headers

      expect(response).to have_http_status(:unprocessable_entity)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Cannot close a position that is not open')
    end
  end

  private

  def json_response
    JSON.parse(response.body)
  end
end
