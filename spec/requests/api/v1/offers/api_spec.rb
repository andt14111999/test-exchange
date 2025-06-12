# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Offers::Api, type: :request do
  describe 'GET /api/v1/offers' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/offers'

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'returns all active offers' do
        user = create(:user)
        create_list(:offer, 3, deleted: false, disabled: false)
        create(:offer, deleted: true) # should not be returned
        create(:offer, disabled: true) # should be returned as it's not deleted

        get '/api/v1/offers', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(4) # 3 active + 1 disabled
      end

      it 'filters offers by offer_type' do
        user = create(:user)
        create_list(:offer, 2, :buy, deleted: false)
        create_list(:offer, 3, :sell, deleted: false)

        get '/api/v1/offers', params: { offer_type: 'buy' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(2)
        expect(json_response.pluck('offer_type')).to all(eq('buy'))
      end

      it 'filters offers by coin_currency' do
        user = create(:user)
        create_list(:offer, 2, coin_currency: 'BTC', deleted: false)
        create_list(:offer, 3, coin_currency: 'USDT', deleted: false)

        get '/api/v1/offers', params: { coin_currency: 'BTC' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(2)
        expect(json_response.pluck('coin_currency')).to all(eq('BTC'))
      end

      it 'filters offers by currency' do
        user = create(:user)
        create_list(:offer, 2, currency: 'USD', deleted: false)
        create_list(:offer, 3, currency: 'VND', deleted: false)

        get '/api/v1/offers', params: { currency: 'USD' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(2)
        expect(json_response.pluck('currency')).to all(eq('USD'))
      end

      it 'filters offers by country_code' do
        user = create(:user)
        create_list(:offer, 2, country_code: 'US', deleted: false)
        create_list(:offer, 3, country_code: 'vn', deleted: false)

        get '/api/v1/offers', params: { country_code: 'US' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(2)
        expect(json_response.pluck('country_code')).to all(eq('US'))
      end

      it 'sorts offers by price ascending' do
        user = create(:user)
        create(:offer, price: 50000.0, deleted: false)
        create(:offer, price: 49000.0, deleted: false)
        create(:offer, price: 51000.0, deleted: false)

        get '/api/v1/offers', params: { sort: 'price_asc' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        prices = json_response.pluck('price')
        expect(prices).to eq(prices.sort)
      end

      it 'sorts offers by price descending' do
        user = create(:user)
        create(:offer, price: 50000.0, deleted: false)
        create(:offer, price: 49000.0, deleted: false)
        create(:offer, price: 51000.0, deleted: false)

        get '/api/v1/offers', params: { sort: 'price_desc' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        prices = json_response.pluck('price')
        expect(prices).to eq(prices.sort.reverse)
      end

      it 'sorts offers by newest first' do
        user = create(:user)
        # Create offers with different creation times
        old_offer = create(:offer, deleted: false, created_at: 3.days.ago)
        middle_offer = create(:offer, deleted: false, created_at: 2.days.ago)
        new_offer = create(:offer, deleted: false, created_at: 1.day.ago)

        get '/api/v1/offers', params: { sort: 'newest' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(3)
        expect(json_response.first['id'].to_i).to eq(new_offer.id)
        expect(json_response.last['id'].to_i).to eq(old_offer.id)
      end

      it 'sorts offers by oldest first' do
        user = create(:user)
        # Create offers with different creation times
        old_offer = create(:offer, deleted: false, created_at: 3.days.ago)
        middle_offer = create(:offer, deleted: false, created_at: 2.days.ago)
        new_offer = create(:offer, deleted: false, created_at: 1.day.ago)

        get '/api/v1/offers', params: { sort: 'oldest' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(3)
        expect(json_response.first['id'].to_i).to eq(old_offer.id)
        expect(json_response.last['id'].to_i).to eq(new_offer.id)
      end

      it 'returns 400 error when sort parameter is invalid' do
        user = create(:user)

        get '/api/v1/offers', params: { sort: 'invalid_sort' }, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end

      it 'defaults to price_asc sorting when no sort parameter is provided' do
        user = create(:user)
        create(:offer, price: 50000.0, deleted: false)
        create(:offer, price: 49000.0, deleted: false)
        create(:offer, price: 51000.0, deleted: false)

        get '/api/v1/offers', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        prices = json_response.pluck('price')
        expect(prices).to eq(prices.sort)
      end

      it 'paginates offers' do
        user = create(:user)
        create_list(:offer, 25, deleted: false)

        get '/api/v1/offers', params: { page: 2, per_page: 10 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(10)
      end
    end
  end

  describe 'GET /api/v1/offers/merchant' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/offers/merchant'

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it "returns current user's offers excluding deleted ones" do
        user = create(:user)
        create_list(:offer, 3, user: user, deleted: false)
        create(:offer, user: user, deleted: true) # should not be returned
        create(:offer, deleted: false) # another user's offer, should not be returned

        get '/api/v1/offers/merchant', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(3)
        user_ids = json_response.map { |o| o['user_id'] }.uniq
        expect(user_ids.first.to_i).to eq(user.id)
      end
    end
  end

  describe 'GET /api/v1/offers/:id' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        offer = create(:offer)
        get "/api/v1/offers/#{offer.id}"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'returns offer details' do
        user = create(:user)
        offer = create(:offer)

        get "/api/v1/offers/#{offer.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response['id'].to_i).to eq(offer.id)
        expect(json_response['offer_type']).to eq(offer.offer_type)
        expect(json_response['coin_currency']).to eq(offer.coin_currency)
        expect(json_response['currency']).to eq(offer.currency)
        expect(json_response['price']).to eq(offer.price.to_s)
      end

      it 'returns 404 for non-existent offer' do
        user = create(:user)
        get '/api/v1/offers/non-existent-id', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'POST /api/v1/offers' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/offers', params: { offer_type: 'buy' }

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'creates a new offer with valid parameters' do
        user = create(:user)
        payment_method = create(:payment_method)

        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)
        allow_any_instance_of(User).to receive(:max_active_offers).and_return(5)
        allow_any_instance_of(ActiveRecord::Relation).to receive(:count).and_return(0)

        allow_any_instance_of(Offer).to receive(:save).and_return(true)

        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.001',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US',
          terms_of_trade: 'Please transfer the exact amount',
          bank_names: [ 'Bank A', 'Bank B' ]
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:created)
        json_response = JSON.parse(response.body)
        expect(json_response['offer_type']).to eq('buy')
        expect(json_response['coin_currency']).to eq('BTC')
        expect(json_response['price']).to eq('50000.0')
      end

      it 'uses margin instead of fixed price when margin is provided' do
        user = create(:user)
        payment_method = create(:payment_method)

        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)
        allow_any_instance_of(User).to receive(:max_active_offers).and_return(5)
        allow_any_instance_of(ActiveRecord::Relation).to receive(:count).and_return(0)
        allow_any_instance_of(Offer).to receive(:update_price_from_market!).and_return(true)

        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          margin: '0.05',
          min_amount: '0.001',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US'
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:created)
        json_response = JSON.parse(response.body)
        expect(json_response['margin']).to eq('0.05')
      end

      it 'returns error when max active offers limit is reached' do
        user = create(:user)

        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)
        allow_any_instance_of(User).to receive(:max_active_offers).and_return(5)
        allow_any_instance_of(ActiveRecord::Relation).to receive(:count).and_return(5)

        payment_method = create(:payment_method)
        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.001',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US'
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('You can have at most 5 active offers')
      end

      it 'returns error when user cannot create offer due to KYC/level requirements' do
        user = create(:user)
        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(false)

        payment_method = create(:payment_method)
        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.001',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US'
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Account not verified or insufficient level')
      end

      it 'returns error when min_amount > max_amount' do
        user = create(:user)
        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)

        payment_method = create(:payment_method)
        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.2',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US'
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Minimum amount must be less than maximum amount')
      end

      it 'returns error when max_amount > total_amount' do
        user = create(:user)
        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)

        payment_method = create(:payment_method)
        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.001',
          max_amount: '2.0',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US'
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Maximum amount must be less than total amount')
      end

      it 'returns error when offer validation fails' do
        user = create(:user)
        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)
        allow_any_instance_of(Offer).to receive(:update).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Validation error' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        payment_method = create(:payment_method)
        params = {
          offer_type: 'invalid_type', # invalid offer type
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.001',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US'
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end

      it 'returns error when offer save fails with validation errors' do
        user = create(:user)
        payment_method = create(:payment_method)

        # Configure mocks
        allow_any_instance_of(User).to receive(:can_create_offer?).and_return(true)
        allow_any_instance_of(User).to receive(:max_active_offers).and_return(5)
        allow_any_instance_of(ActiveRecord::Relation).to receive(:count).and_return(0)

        # Mock offer.save to return false and provide error messages
        allow_any_instance_of(Offer).to receive(:save).and_return(false)
        allow_any_instance_of(Offer).to receive(:errors).and_return(
          instance_double(ActiveModel::Errors, full_messages: [ 'Price must be greater than 0', 'Terms of trade is too long' ])
        )

        params = {
          offer_type: 'buy',
          coin_currency: 'BTC',
          currency: 'USD',
          price: '50000.0',
          min_amount: '0.001',
          max_amount: '0.1',
          total_amount: '1.0',
          payment_method_id: payment_method.id,
          payment_time: 30,
          payment_details: { bank_account: '1234567890' },
          country_code: 'US',
          terms_of_trade: 'Please transfer the exact amount',
          bank_names: [ 'Bank A', 'Bank B' ]
        }

        post '/api/v1/offers', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Price must be greater than 0')
        expect(json_response['error']).to include('Terms of trade is too long')
      end
    end
  end

  describe 'PUT /api/v1/offers/:id' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        offer = create(:offer)
        put "/api/v1/offers/#{offer.id}", params: { price: '51000.0' }

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'updates offer with valid parameters' do
        user = create(:user)
        offer = create(:offer, user: user, price: '50000.0')

        put "/api/v1/offers/#{offer.id}",
            params: { price: '51000.0', terms_of_trade: 'Updated terms' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response['price']).to eq('51000.0')
        expect(json_response['terms_of_trade']).to eq('Updated terms')
      end

      it 'returns error when trying to update a deleted offer' do
        user = create(:user)
        offer = create(:offer, :deleted, :disabled, user: user)

        put "/api/v1/offers/#{offer.id}",
            params: { price: '51000.0' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Cannot update a deleted offer')
      end

      it 'returns error when trying to change offer_type' do
        user = create(:user)
        offer = create(:offer, :buy, user: user)

        put "/api/v1/offers/#{offer.id}",
            params: { offer_type: 'sell' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Cannot change offer type')
      end

      it 'returns error when min_amount > max_amount (both provided)' do
        user = create(:user)
        offer = create(:offer, user: user, min_amount: 0.001, max_amount: 0.1)

        put "/api/v1/offers/#{offer.id}",
            params: { min_amount: '0.2', max_amount: '0.05' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Minimum amount must be less than maximum amount')
      end

      it 'returns error when min_amount > existing max_amount' do
        user = create(:user)
        offer = create(:offer, user: user, min_amount: 0.001, max_amount: 0.1)

        put "/api/v1/offers/#{offer.id}",
            params: { min_amount: '0.2' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Minimum amount must be less than maximum amount')
      end

      it 'returns error when max_amount < existing min_amount' do
        user = create(:user)
        offer = create(:offer, user: user, min_amount: 0.05, max_amount: 0.1)

        put "/api/v1/offers/#{offer.id}",
            params: { max_amount: '0.01' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Maximum amount must be greater than minimum amount')
      end

      it 'returns error when max_amount > total_amount (both provided)' do
        user = create(:user)
        offer = create(:offer, user: user, max_amount: 0.1, total_amount: 1.0)

        put "/api/v1/offers/#{offer.id}",
            params: { max_amount: '2.0', total_amount: '1.5' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Maximum amount must be less than total amount')
      end

      it 'returns error when max_amount > existing total_amount' do
        user = create(:user)
        offer = create(:offer, user: user, max_amount: 0.1, total_amount: 1.0)

        put "/api/v1/offers/#{offer.id}",
            params: { max_amount: '2.0' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Maximum amount must be less than total amount')
      end

      it 'returns error when total_amount < existing max_amount' do
        user = create(:user)
        offer = create(:offer, user: user, max_amount: 0.5, total_amount: 1.0)

        put "/api/v1/offers/#{offer.id}",
            params: { total_amount: '0.4' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Total amount must be greater than maximum amount')
      end

      it 'updates price using margin when margin is provided' do
        user = create(:user)
        offer = create(:offer, user: user)
        allow_any_instance_of(Offer).to receive(:update_price_from_market!).and_return(true)

        put "/api/v1/offers/#{offer.id}",
            params: { margin: '0.05', price: '50000.0' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        json_response = JSON.parse(response.body)
        expect(json_response['margin']).to eq('0.05')
      end

      it 'returns error when offer validation fails' do
        user = create(:user)
        offer = create(:offer, user: user)
        allow_any_instance_of(Offer).to receive(:update).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Validation error' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        put "/api/v1/offers/#{offer.id}",
            params: { price: '-1.0' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end

      it 'returns 404 when offer is not found' do
        user = create(:user)
        put "/api/v1/offers/non-existent-id",
            params: { price: '51000.0' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns unauthorized when trying to update another user\'s offer' do
        user1 = create(:user)
        user2 = create(:user)
        offer = create(:offer, user: user1)

        put "/api/v1/offers/#{offer.id}",
            params: { price: '51000.0' },
            headers: auth_headers(user2)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'PUT /api/v1/offers/:id/disable' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        offer = create(:offer)
        put "/api/v1/offers/#{offer.id}/disable"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'disables an active offer' do
        user = create(:user)
        offer = create(:offer, user: user, disabled: false)
        allow_any_instance_of(Offer).to receive(:disable!).and_return(true)

        put "/api/v1/offers/#{offer.id}/disable", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error when trying to disable a deleted offer' do
        user = create(:user)
        offer = create(:offer, :deleted, user: user)

        # Mock the deleted? method for this test
        allow_any_instance_of(Offer).to receive(:deleted?).and_return(true)

        put "/api/v1/offers/#{offer.id}/disable", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Cannot disable a deleted offer')
      end

      it 'returns error when trying to disable an already disabled offer' do
        user = create(:user)
        offer = create(:offer, :disabled, user: user)
        allow_any_instance_of(Offer).to receive(:disabled?).and_return(true)

        put "/api/v1/offers/#{offer.id}/disable", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Offer is already disabled')
      end

      it 'returns error when disable operation fails' do
        user = create(:user)
        offer = create(:offer, user: user, disabled: false)
        allow_any_instance_of(Offer).to receive(:disable!).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Error disabling offer' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        put "/api/v1/offers/#{offer.id}/disable", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end
    end
  end

  describe 'PUT /api/v1/offers/:id/enable' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        offer = create(:offer, :disabled)
        put "/api/v1/offers/#{offer.id}/enable"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'enables a disabled offer' do
        user = create(:user)
        offer = create(:offer, :disabled, user: user)
        allow_any_instance_of(Offer).to receive(:disabled?).and_return(true)
        allow_any_instance_of(Offer).to receive(:available_amount).and_return(1.0)
        allow_any_instance_of(Offer).to receive(:min_amount).and_return(0.1)
        allow_any_instance_of(Offer).to receive(:enable!).and_return(true)

        put "/api/v1/offers/#{offer.id}/enable", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error when trying to enable a deleted offer' do
        user = create(:user)
        offer = create(:offer, :deleted, :disabled, user: user)

        put "/api/v1/offers/#{offer.id}/enable", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Cannot enable a deleted offer')
      end

      it 'returns error when trying to enable an already enabled offer' do
        user = create(:user)
        offer = create(:offer, user: user, disabled: false)
        allow_any_instance_of(Offer).to receive(:disabled?).and_return(false)

        put "/api/v1/offers/#{offer.id}/enable", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Offer is not disabled')
      end

      it 'returns error when available amount is too low' do
        user = create(:user)
        offer = create(:offer, :disabled, user: user)
        allow_any_instance_of(Offer).to receive(:disabled?).and_return(true)
        allow_any_instance_of(Offer).to receive(:available_amount).and_return(0.001)
        allow_any_instance_of(Offer).to receive(:min_amount).and_return(0.01)

        put "/api/v1/offers/#{offer.id}/enable", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Available amount is too low to enable offer')
      end

      it 'returns error when enable operation fails' do
        user = create(:user)
        offer = create(:offer, :disabled, user: user)
        allow_any_instance_of(Offer).to receive(:disabled?).and_return(true)
        allow_any_instance_of(Offer).to receive(:available_amount).and_return(1.0)
        allow_any_instance_of(Offer).to receive(:min_amount).and_return(0.1)
        allow_any_instance_of(Offer).to receive(:enable!).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Error enabling offer' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        put "/api/v1/offers/#{offer.id}/enable", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end
    end
  end

  describe 'DELETE /api/v1/offers/:id' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        offer = create(:offer)
        delete "/api/v1/offers/#{offer.id}"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'deletes an offer' do
        user = create(:user)
        offer = create(:offer, user: user, deleted: false)
        allow_any_instance_of(Offer).to receive(:delete!).and_return(true)

        delete "/api/v1/offers/#{offer.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error when trying to delete an already deleted offer' do
        user = create(:user)
        offer = create(:offer, :deleted, user: user)

        delete "/api/v1/offers/#{offer.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Offer is already deleted')
      end

      it 'returns error when delete operation fails' do
        user = create(:user)
        offer = create(:offer, user: user, deleted: false)
        allow_any_instance_of(Offer).to receive(:delete!).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Error deleting offer' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        delete "/api/v1/offers/#{offer.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end
    end
  end

  describe 'PUT /api/v1/offers/:id/online_status' do
    context 'when not authenticated' do
      it 'returns unauthorized error' do
        offer = create(:offer)
        put "/api/v1/offers/#{offer.id}/online_status", params: { online: true }

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when authenticated' do
      it 'sets offer online' do
        user = create(:user)
        offer = create(:offer, user: user, online: false)
        allow_any_instance_of(Offer).to receive(:set_online!).and_return(true)

        put "/api/v1/offers/#{offer.id}/online_status",
            params: { online: true },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'sets offer offline' do
        user = create(:user)
        offer = create(:offer, user: user, online: true)
        allow_any_instance_of(Offer).to receive(:set_offline!).and_return(true)

        put "/api/v1/offers/#{offer.id}/online_status",
            params: { online: false },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error when trying to change online status for a deleted offer' do
        user = create(:user)
        offer = create(:offer, :deleted, user: user)
        allow_any_instance_of(Offer).to receive(:deleted?).and_return(true)

        put "/api/v1/offers/#{offer.id}/online_status",
            params: { online: true },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('Cannot change online status for a deleted offer')
      end

      it 'returns error when set_online operation fails' do
        user = create(:user)
        offer = create(:offer, user: user, online: false)
        allow_any_instance_of(Offer).to receive(:set_online!).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Error setting offer online' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        put "/api/v1/offers/#{offer.id}/online_status",
            params: { online: true },
            headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end

      it 'returns error when set_offline operation fails' do
        user = create(:user)
        offer = create(:offer, user: user, online: true)
        allow_any_instance_of(Offer).to receive(:set_offline!).and_return(false)

        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Error setting offer offline' ])
        allow_any_instance_of(Offer).to receive(:errors).and_return(errors)

        put "/api/v1/offers/#{offer.id}/online_status",
            params: { online: false },
            headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        json_response = JSON.parse(response.body)
        expect(json_response).to have_key('error')
      end
    end
  end

  def auth_headers(user = nil)
    user ||= create(:user)
    token = JWT.encode(
      {
        user_id: user.id,
        email: user.email,
        exp: 24.hours.from_now.to_i
      },
      Rails.application.secret_key_base,
      'HS256'
    )

    { 'Authorization' => "Bearer #{token}" }
  end
end
