# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::FiatDeposits::Api, type: :request do
  describe 'GET /api/v1/fiat_deposits' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        get '/api/v1/fiat_deposits'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns all fiat deposits for the user' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        deposits = create_list(:fiat_deposit, 3, user: user, fiat_account: fiat_account)

        get '/api/v1/fiat_deposits', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(3)
      end

      it 'filters deposits by status' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        create(:fiat_deposit, user: user, fiat_account: fiat_account, status: 'pending')
        create(:fiat_deposit, user: user, fiat_account: fiat_account, status: 'processed')

        get '/api/v1/fiat_deposits', params: { status: 'pending' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
        expect(json_response[0]['status']).to eq('pending')
      end

      it 'filters deposits by currency' do
        user = create(:user)
        vnd_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)
        php_account = create(:fiat_account, user: user, currency: 'PHP', balance: 1000)
        create(:fiat_deposit, user: user, fiat_account: vnd_account, currency: 'VND')
        create(:fiat_deposit, user: user, fiat_account: php_account, currency: 'PHP')

        get '/api/v1/fiat_deposits', params: { currency: 'VND' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
        expect(json_response[0]['currency']).to eq('VND')
      end

      it 'paginates the results' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 5000, currency: 'VND')
        create_list(:fiat_deposit, 25, user: user, fiat_account: fiat_account)

        get '/api/v1/fiat_deposits', params: { page: 2, per_page: 10 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(10)
      end
    end
  end

  describe 'GET /api/v1/fiat_deposits/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        get '/api/v1/fiat_deposits/123'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns the deposit details' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account)

        get "/api/v1/fiat_deposits/#{deposit.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['id'].to_s).to eq(deposit.id.to_s)
      end

      it 'returns 404 if deposit does not exist' do
        user = create(:user)

        get "/api/v1/fiat_deposits/non_existent_id", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 404 if deposit belongs to another user' do
        user = create(:user)
        other_user = create(:user)
        fiat_account = create(:fiat_account, user: other_user, balance: 1000, currency: 'VND')
        deposit = create(:fiat_deposit, user: other_user, fiat_account: fiat_account)

        get "/api/v1/fiat_deposits/#{deposit.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'POST /api/v1/fiat_deposits' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        post '/api/v1/fiat_deposits'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'creates a new deposit with valid offer' do
        user = create(:user)
        seller = create(:user)
        offer = create(:offer, :sell, user: seller, currency: 'VND', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          offer_id: offer.id,
          memo: 'Test Deposit'
        }

        expect {
          post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)
        }.to change(FiatDeposit, :count).by(1).and change(Trade, :count).by(1)

        expect(response).to have_http_status(:created)
        expect(json_response['currency']).to eq('VND')
        expect(json_response['fiat_amount'].to_f).to eq(500000.0)
        expect(json_response['status']).to eq('pending')
      end

      it 'creates a new deposit with existing fiat account' do
        user = create(:user)
        seller = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 0)
        offer = create(:offer, :sell, user: seller, currency: 'VND', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          fiat_account_id: fiat_account.id,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:created)
        deposit = FiatDeposit.last
        expect(deposit.fiat_account_id).to eq(fiat_account.id)
      end

      it 'returns error if offer is not active' do
        user = create(:user)
        seller = create(:user)
        offer = create(:offer, :sell, user: seller, currency: 'VND', price: 50000, coin_currency: 'BTC', disabled: true)
        allow_any_instance_of(Offer).to receive(:active?).and_return(false)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Offer is not active')
      end

      it 'returns error if user tries to use their own offer' do
        user = create(:user)
        offer = create(:offer, :sell, user: user, currency: 'VND', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Cannot create deposit with your own offer')
      end

      it 'returns error if currency does not match offer currency' do
        user = create(:user)
        seller = create(:user)
        offer = create(:offer, :sell, user: seller, currency: 'PHP', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Offer currency does not match deposit currency')
      end

      it 'returns error if fiat account currency does not match deposit currency' do
        user = create(:user)
        seller = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'PHP', balance: 0)
        offer = create(:offer, :sell, user: seller, currency: 'VND', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          fiat_account_id: fiat_account.id,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Fiat account currency does not match deposit currency')
      end

      it 'returns error when trade cannot be saved' do
        user = create(:user)
        seller = create(:user)
        offer = create(:offer, :sell, user: seller, currency: 'VND', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        # Make Trade.save return false to simulate a save failure
        allow_any_instance_of(Trade).to receive(:save).and_return(false)
        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Trade save error' ])
        allow_any_instance_of(Trade).to receive(:errors).and_return(errors)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Trade save error')
      end

      it 'returns error when deposit cannot be saved' do
        user = create(:user)
        seller = create(:user)
        offer = create(:offer, :sell, user: seller, currency: 'VND', price: 50000, coin_currency: 'BTC')
        allow_any_instance_of(Offer).to receive(:active?).and_return(true)

        # Make Trade.save return true but FiatDeposit.save return false
        allow_any_instance_of(Trade).to receive(:save).and_return(true)
        allow_any_instance_of(FiatDeposit).to receive(:save).and_return(false)
        errors = instance_double(ActiveModel::Errors, full_messages: [ 'Deposit save error' ])
        allow_any_instance_of(FiatDeposit).to receive(:errors).and_return(errors)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500000,
          offer_id: offer.id
        }

        post '/api/v1/fiat_deposits', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Deposit save error')
      end
    end
  end

  describe 'PUT /api/v1/fiat_deposits/:id/money_sent' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        put '/api/v1/fiat_deposits/123/money_sent'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'marks a deposit as money sent' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, :pending, user: user, fiat_account: fiat_account, payable: trade)

        allow_any_instance_of(Trade).to receive(:may_mark_as_paid?).and_return(true)
        allow_any_instance_of(Trade).to receive(:mark_as_paid!).and_return(true)
        allow_any_instance_of(Trade).to receive(:add_payment_proof!).and_return(true)

        params = {
          payment_proof_url: 'https://example.com/proof.jpg',
          payment_description: 'Payment sent via bank transfer'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/money_sent", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        deposit.reload
        expect(deposit.status).to eq('money_sent')
        expect(deposit.payment_proof_url).to eq('https://example.com/proof.jpg')
        expect(deposit.payment_description).to eq('Payment sent via bank transfer')
      end

      it 'adds additional proof to money_sent deposit' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account, payable: trade, status: 'money_sent', payment_proof_url: 'https://example.com/original.jpg', payment_description: 'Initial payment')

        allow_any_instance_of(Trade).to receive(:add_payment_proof!).and_return(true)

        params = {
          payment_proof_url: 'https://example.com/additional.jpg',
          payment_description: 'Additional proof',
          additional_proof: true
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/money_sent", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        deposit.reload
        expect(deposit.payment_proof_url).to eq('https://example.com/additional.jpg')
        expect(deposit.payment_description).to include('Initial payment')
        expect(deposit.payment_description).to include('Additional proof')
      end

      it 'returns error if deposit is not in pending state' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account, payable: trade, status: 'awaiting')

        params = {
          payment_proof_url: 'https://example.com/proof.jpg',
          payment_description: 'Payment sent via bank transfer'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/money_sent", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Deposit must be in pending state')
      end

      it 'allows updating payment proof without marking as sent' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, :pending, user: user, fiat_account: fiat_account, payable: trade)

        allow_any_instance_of(Trade).to receive(:add_payment_proof!).and_return(true)

        params = {
          payment_proof_url: 'https://example.com/proof.jpg',
          payment_description: 'Payment sent via bank transfer',
          mark_as_sent: false
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/money_sent", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        deposit.reload
        expect(deposit.status).to eq('pending') # Still pending, not money_sent
        expect(deposit.payment_proof_url).to eq('https://example.com/proof.jpg')
      end

      it 'returns error when deposit cannot be marked as money_sent' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, :pending, user: user, fiat_account: fiat_account, payable: trade)

        allow_any_instance_of(Trade).to receive(:add_payment_proof!).and_return(true)
        allow_any_instance_of(FiatDeposit).to receive(:money_sent!).and_return(true)
        allow_any_instance_of(FiatDeposit).to receive(:money_sent?).and_return(false)

        params = {
          payment_proof_url: 'https://example.com/proof.jpg',
          payment_description: 'Payment sent via bank transfer'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/money_sent", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to mark deposit as money sent')
      end
    end
  end

  describe 'PUT /api/v1/fiat_deposits/:id/cancel' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        put '/api/v1/fiat_deposits/123/cancel'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'cancels a deposit in awaiting state' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, :awaiting, user: user, fiat_account: fiat_account, payable: trade)

        allow_any_instance_of(Trade).to receive(:may_cancel?).and_return(true)
        allow_any_instance_of(Trade).to receive(:cancel!).and_return(true)

        params = {
          cancel_reason: 'Changed my mind'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/cancel", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        deposit.reload
        expect(deposit.status).to eq('cancelled')
        expect(deposit.cancel_reason).to eq('Changed my mind')
      end

      it 'cancels a deposit in pending state' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, :pending, user: user, fiat_account: fiat_account, payable: trade)

        allow_any_instance_of(Trade).to receive(:may_cancel?).and_return(true)
        allow_any_instance_of(Trade).to receive(:cancel!).and_return(true)

        put "/api/v1/fiat_deposits/#{deposit.id}/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        deposit.reload
        expect(deposit.status).to eq('cancelled')
        expect(deposit.cancel_reason).to eq('Cancelled by user')
      end

      it 'returns error if deposit cannot be cancelled' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account, payable: trade, status: 'money_sent')

        put "/api/v1/fiat_deposits/#{deposit.id}/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Only deposits in awaiting or pending state can be cancelled')
      end

      it 'returns error when cancel operation fails' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        trade = create(:trade, buyer: user)
        deposit = create(:fiat_deposit, :pending, user: user, fiat_account: fiat_account, payable: trade)

        # Make the cancel! method return false to simulate failure
        allow_any_instance_of(FiatDeposit).to receive(:cancel!).and_return(false)

        put "/api/v1/fiat_deposits/#{deposit.id}/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to cancel deposit')
      end
    end
  end

  describe 'PUT /api/v1/fiat_deposits/:id/verify_ownership' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        put '/api/v1/fiat_deposits/123/verify_ownership'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'verifies ownership successfully' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account, status: 'ownership_verifying')

        # Stub verify_ownership! method to simulate successful verification
        allow_any_instance_of(FiatDeposit).to receive(:verify_ownership!).and_return(true)

        params = {
          ownership_proof_url: 'https://example.com/ownership.jpg',
          sender_name: 'John Doe',
          sender_account_number: '9876543210'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/verify_ownership", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error if verification fails' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account, status: 'ownership_verifying')

        # Stub verify_ownership! method to simulate failed verification
        allow_any_instance_of(FiatDeposit).to receive(:verify_ownership!).and_return(false)

        params = {
          ownership_proof_url: 'https://example.com/ownership.jpg',
          sender_name: 'John Doe',
          sender_account_number: '9876543210'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/verify_ownership", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to include('Failed to verify ownership')
      end

      it 'returns error if deposit does not require ownership verification' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        deposit = create(:fiat_deposit, user: user, fiat_account: fiat_account, status: 'pending')

        params = {
          ownership_proof_url: 'https://example.com/ownership.jpg',
          sender_name: 'John Doe',
          sender_account_number: '9876543210'
        }

        put "/api/v1/fiat_deposits/#{deposit.id}/verify_ownership", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to include('Deposit does not require ownership verification')
      end
    end
  end
end
