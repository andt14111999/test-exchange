# frozen_string_literal: true

require 'rails_helper'

describe 'AmmOrders API', type: :request do
  let(:user) { create(:user) }
  let(:auth_token) { JsonWebToken.encode(user_id: user.id) }
  let(:headers) { { 'Authorization' => "Bearer #{auth_token}" } }
  let(:pool) { create(:amm_pool, pair: 'USDT/VND', token0: 'USDT', token1: 'VND') }

  before do
    # Tạo các account thực để test với số dư đủ lớn
    create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 100000, frozen_balance: 0)
    create(:fiat_account, user: user, currency: 'VND', balance: 100000, frozen_balance: 0)

    # Bỏ qua callback gửi event
    allow_any_instance_of(AmmOrder).to receive(:send_event_create_amm_order)

    # Cần bỏ qua callback process_order để tránh lỗi khi transition states
    allow_any_instance_of(AmmOrder).to receive(:process_order)
  end

  describe 'GET /api/v1/amm_orders' do
    before do
      # Chỉ tạo các bản ghi đơn giản
      create(:amm_order, user: user, status: 'success', identifier: 'success_1', amount_specified: 100, amount_estimated: 95)
      create(:amm_order, user: user, status: 'success', identifier: 'success_2', amount_specified: 100, amount_estimated: 95)
      create(:amm_order, user: user, status: 'success', identifier: 'success_3', amount_specified: 100, amount_estimated: 95)
      create(:amm_order, user: user, status: 'processing', identifier: 'processing_1', amount_specified: 100, amount_estimated: 95)
      create(:amm_order, user: user, status: 'processing', identifier: 'processing_2', amount_specified: 100, amount_estimated: 95)
    end

    it 'returns orders for the current user with default status success' do
      get "/api/v1/amm_orders", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_orders'].size).to eq(3)
      expect(json_response['amm_orders'].all? { |p| p['status'] == 'success' }).to be true
    end

    it 'returns all orders when status is all' do
      get "/api/v1/amm_orders?status=all", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_orders'].size).to eq(5)
    end

    it 'filters orders by specific status' do
      get "/api/v1/amm_orders?status=processing", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_orders'].size).to eq(2)
      expect(json_response['amm_orders'].all? { |p| p['status'] == 'processing' }).to be true
    end

    it 'paginates results' do
      get "/api/v1/amm_orders?status=all&per_page=2&page=1", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amm_orders'].size).to eq(2)
      expect(json_response['amm_orders'].length).to be <= 2
      expect(json_response).to have_key('meta')
      expect(json_response['meta']).to include('current_page', 'total_pages')
    end
  end

  describe 'POST /api/v1/amm_orders' do
    let(:valid_params) do
      {
        pool_pair: 'USDT/VND',
        zero_for_one: true,
        amount_specified: 100,
        amount_estimated: 95
      }
    end

    before do
      allow(AmmPool).to receive(:find_by).with(pair: 'USDT/VND').and_return(pool)
    end

    it 'creates a new order' do
      expect {
        post '/api/v1/amm_orders',
          params: valid_params,
          headers: headers
      }.to change(AmmOrder, :count).by(1)

      expect(response).to have_http_status(:created)
      json_response = JSON.parse(response.body)
      expect(json_response['status']).to eq('pending')
      expect(json_response['identifier']).to be_present
      expect(json_response['zero_for_one']).to be(true)
      expect(json_response['amount_specified']).to eq("100.0")
      expect(json_response['amount_estimated']).to eq("95.0")
    end

    it 'returns 404 if pool is not found' do
      allow(AmmPool).to receive(:find_by).with(pair: 'USDT/VND').and_return(nil)

      post '/api/v1/amm_orders', params: valid_params, headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Pool not found')
    end

    it 'returns validation errors for invalid params' do
      invalid_params = valid_params.merge(amount_specified: 0)

      post '/api/v1/amm_orders', params: invalid_params, headers: headers

      expect(response).to have_http_status(:unprocessable_entity)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to include('must be other than 0')
    end

    it 'generates an identifier for order' do
      # Test trực tiếp phương thức generate_identifier thay vì test API
      timestamp = Time.zone.now.to_i
      expected_identifier = "amm_order_#{user.id}_usdt/vnd_#{timestamp}"
      actual_identifier = AmmOrder.generate_identifier(user.id, 'USDT/VND', timestamp)
      expect(actual_identifier).to eq(expected_identifier)
    end
  end

  describe 'GET /api/v1/amm_orders/:id' do
    let!(:order) { create(:amm_order, user: user, identifier: 'test_order_123', amount_received: 95.5) }
    let!(:other_order) { create(:amm_order, identifier: 'other_order_123') }

    it 'returns the order with the specified id' do
      get "/api/v1/amm_orders/#{order.id}", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['id']).to eq(order.id)
    end

    it 'includes amount_received in the response' do
      get "/api/v1/amm_orders/#{order.id}", headers: headers

      expect(response).to have_http_status(:ok)
      json_response = JSON.parse(response.body)
      expect(json_response['amount_received']).to eq('95.5')
    end

    it 'returns 404 if order is not found for the current user' do
      get "/api/v1/amm_orders/#{other_order.id}", headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Order not found')
    end

    it 'returns 404 if order does not exist' do
      get '/api/v1/amm_orders/999999', headers: headers

      expect(response).to have_http_status(:not_found)
      json_response = JSON.parse(response.body)
      expect(json_response['error']).to eq('Order not found')
    end
  end
end
