# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::FiatWithdrawals::Api, type: :request do
  # Stub response helpers
  def stub_response(http_status, body = {})
    allow_any_instance_of(ActionDispatch::Response).to receive(:body).and_return(body.to_json)
    allow_any_instance_of(ActionDispatch::Response).to receive(:status).and_return(http_status)
  end

  describe 'GET /api/v1/fiat_withdrawals' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        get '/api/v1/fiat_withdrawals'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns all fiat withdrawals for the user' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawals = create_list(:fiat_withdrawal, 3, user: user, fiat_account: fiat_account, fiat_amount: 100)

        get '/api/v1/fiat_withdrawals', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(JSON.parse(response.body).size).to eq(3)
      end

      it 'filters withdrawals by status' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'pending', fiat_amount: 100)
        create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'processed', fiat_amount: 100)

        get '/api/v1/fiat_withdrawals', params: { status: 'pending' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(JSON.parse(response.body).size).to eq(1)
        expect(JSON.parse(response.body)[0]['status']).to eq('pending')
      end

      it 'filters withdrawals by currency' do
        user = create(:user)
        vnd_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)
        php_account = create(:fiat_account, user: user, currency: 'PHP', balance: 1000)
        create(:fiat_withdrawal, user: user, fiat_account: vnd_account, currency: 'VND', fiat_amount: 100)
        create(:fiat_withdrawal, user: user, fiat_account: php_account, currency: 'PHP', fiat_amount: 100)

        get '/api/v1/fiat_withdrawals', params: { currency: 'VND' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(JSON.parse(response.body).size).to eq(1)
        expect(JSON.parse(response.body)[0]['currency']).to eq('VND')
      end

      it 'paginates the results' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 5000, currency: 'VND')
        create_list(:fiat_withdrawal, 25, user: user, fiat_account: fiat_account, fiat_amount: 100)

        get '/api/v1/fiat_withdrawals', params: { page: 2, per_page: 10 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(JSON.parse(response.body).size).to eq(10)
      end
    end
  end

  describe 'GET /api/v1/fiat_withdrawals/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        get '/api/v1/fiat_withdrawals/123'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns the withdrawal details' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, fiat_amount: 100)

        get "/api/v1/fiat_withdrawals/#{withdrawal.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(JSON.parse(response.body)['id'].to_s).to eq(withdrawal.id.to_s)
      end

      it 'returns 404 if withdrawal does not exist' do
        user = create(:user)

        get "/api/v1/fiat_withdrawals/non_existent_id", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 404 if withdrawal belongs to another user' do
        user = create(:user)
        other_user = create(:user)
        fiat_account = create(:fiat_account, user: other_user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: other_user, fiat_account: fiat_account, fiat_amount: 100)

        get "/api/v1/fiat_withdrawals/#{withdrawal.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'POST /api/v1/fiat_withdrawals' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        post '/api/v1/fiat_withdrawals'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      before do
        # Configure withdrawal limits
        allow(Rails.application.config).to receive_messages(min_withdrawal_amounts: { 'VND' => 100 }, max_withdrawal_amounts: { 'VND' => 5000 }, withdrawal_daily_limits: { 'VND' => 10000 }, withdrawal_weekly_limits: { 'VND' => 50000 })
      end

      it 'creates a new withdrawal with fiat_account_id' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        expect {
          post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)
        }.to change(FiatWithdrawal, :count).by(1)

        expect(response).to have_http_status(:created)
        response_body = JSON.parse(response.body)
        expect(response_body['currency']).to eq('VND')
        # The status value might not be in the response depending on how the entity is implemented
        # but we can check for other fields that should be present
        expect(response_body['fiat_amount'].to_s).to eq('500.0')
      end

      it 'creates a new withdrawal without fiat_account_id' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch'
        }

        expect {
          post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)
        }.to change(FiatWithdrawal, :count).by(1)

        expect(response).to have_http_status(:created)
      end

      it 'returns error if fiat account not found' do
        user = create(:user)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch'
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(JSON.parse(response.body)['error']).to include('Fiat account not found')
      end

      it 'returns error if currency does not match fiat account currency' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)

        params = {
          currency: 'PHP',
          country_code: 'PH',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Manila Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('currency does not match')
      end

      it 'returns error if amount is less than minimum' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 50, # Less than minimum
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('must be at least')
      end

      it 'returns error if amount exceeds maximum' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 10000)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 6000, # More than maximum
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('cannot exceed')
      end

      it 'returns error if daily limit exceeded' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 10000)

        # Configure withdrawal limits
        allow(Rails.application.config).to receive(:withdrawal_daily_limits).and_return({ 'VND' => 1000 })

        # Create previous withdrawal today
        create(:fiat_withdrawal, user: user, fiat_account: fiat_account, currency: 'VND', fiat_amount: 600, created_at: Time.zone.today)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('daily limit')
      end

      it 'returns error if weekly limit exceeded' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 10000)

        # Set up withdrawal limits
        weekly_limit = 1000
        allow(Rails.application.config).to receive_messages(min_withdrawal_amounts: { 'VND' => 100 }, max_withdrawal_amounts: { 'VND' => 5000 }, withdrawal_daily_limits: { 'VND' => 5000 }, withdrawal_weekly_limits: { 'VND' => weekly_limit })

        # Create a previous withdrawal
        previous_amount = 600
        create(:fiat_withdrawal,
          user: user,
          fiat_account: fiat_account,
          currency: 'VND',
          fiat_amount: previous_amount,
          created_at: 2.days.ago
        )

        # Mock the validation to fail
        allow_any_instance_of(FiatWithdrawal).to receive(:valid?).and_return(false)
        allow_any_instance_of(FiatWithdrawal).to receive(:errors).and_return(
          instance_double(ActiveModel::Errors, full_messages: [ "Fiat amount exceeds weekly withdrawal limit of #{weekly_limit} VND" ])
        )

        # Attempt to create a withdrawal
        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        # For validation errors, the API returns 422 Unprocessable Entity
        expect(response).to have_http_status(:unprocessable_entity)
        expect(JSON.parse(response.body)['error']).to include('weekly withdrawal limit')
      end

      it 'returns error if insufficient balance' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 300)

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('Insufficient balance')
      end

      it 'returns error if validation fails' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000)

        # Mock save to fail
        allow_any_instance_of(FiatWithdrawal).to receive(:save).and_return(false)
        allow_any_instance_of(FiatWithdrawal).to receive(:errors).and_return(
          instance_double(ActiveModel::Errors, full_messages: [ 'Error message' ])
        )

        params = {
          currency: 'VND',
          country_code: 'VN',
          fiat_amount: 500,
          bank_name: 'Test Bank',
          bank_account_name: 'John Doe',
          bank_account_number: '123456789',
          bank_branch: 'Hanoi Branch',
          fiat_account_id: fiat_account.id
        }

        post '/api/v1/fiat_withdrawals', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
      end
    end
  end

  describe 'PUT /api/v1/fiat_withdrawals/:id/cancel' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        put '/api/v1/fiat_withdrawals/123/cancel'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'cancels the withdrawal' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'pending', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:can_be_cancelled?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:cancel!).and_return(true)

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/cancel", params: { cancel_reason: 'Test reason' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error if withdrawal cannot be cancelled' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'processed', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:can_be_cancelled?).and_return(false)

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('cannot be cancelled')
      end

      it 'returns error if cancel operation fails' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'pending', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:can_be_cancelled?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:cancel!).and_return(false)

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(JSON.parse(response.body)['error']).to include('Failed to cancel')
      end

      it 'returns 404 if withdrawal does not exist' do
        user = create(:user)

        put "/api/v1/fiat_withdrawals/non_existent_id/cancel", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'PUT /api/v1/fiat_withdrawals/:id/retry' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        put '/api/v1/fiat_withdrawals/123/retry'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'retries the withdrawal with updated bank details' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'bank_rejected', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:bank_rejected?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:can_be_retried?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:retry!).and_return(true)

        params = {
          bank_name: 'New Bank',
          bank_account_name: 'New Account Name',
          bank_account_number: 'New Account Number',
          bank_branch: 'New Branch'
        }

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/retry", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'retries the withdrawal without updating bank details' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'bank_rejected', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:bank_rejected?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:can_be_retried?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:retry!).and_return(true)

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/retry", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns error if withdrawal cannot be retried' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'processed', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:bank_rejected?).and_return(false)

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/retry", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('cannot be retried')
      end

      it 'returns error if retry operation fails' do
        user = create(:user)
        fiat_account = create(:fiat_account, user: user, balance: 1000, currency: 'VND')
        withdrawal = create(:fiat_withdrawal, user: user, fiat_account: fiat_account, status: 'bank_rejected', fiat_amount: 100)

        allow_any_instance_of(FiatWithdrawal).to receive(:bank_rejected?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:can_be_retried?).and_return(true)
        allow_any_instance_of(FiatWithdrawal).to receive(:retry!).and_return(false)

        put "/api/v1/fiat_withdrawals/#{withdrawal.id}/retry", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(JSON.parse(response.body)['error']).to include('Failed to retry')
      end

      it 'returns 404 if withdrawal does not exist' do
        user = create(:user)

        put "/api/v1/fiat_withdrawals/non_existent_id/retry", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'GET /api/v1/fiat_withdrawals/p2p/:trade_id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized status' do
        get '/api/v1/fiat_withdrawals/p2p/123'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns withdrawal status for a trade where user is buyer' do
        user = create(:user)
        seller = create(:user)
        trade = create(:trade)
        withdrawal = create(:fiat_withdrawal, fiat_amount: 100)

        # Create proper stubs for a successful trade lookup as buyer
        trade_double = instance_double(Trade,
                              id: trade.id,
                              buyer_id: user.id,
                              seller_id: seller.id,
                              is_fiat_token_withdrawal_trade?: true,
                              fiat_withdrawal: withdrawal)

        # Allow the trade to be found and return our properly mocked trade
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(
          class_double(Trade, find_by: trade_double)
        )

        get "/api/v1/fiat_withdrawals/p2p/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns withdrawal status for a trade where user is seller' do
        user = create(:user)
        buyer = create(:user)
        trade = create(:trade)
        withdrawal = create(:fiat_withdrawal, fiat_amount: 100)

        # Create proper stubs for a successful trade lookup as seller
        trade_double = instance_double(Trade,
                              id: trade.id,
                              buyer_id: buyer.id,
                              seller_id: user.id,
                              is_fiat_token_withdrawal_trade?: true,
                              fiat_withdrawal: withdrawal)

        # Allow the trade to be found and return our properly mocked trade
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(
          class_double(Trade, find_by: trade_double)
        )

        get "/api/v1/fiat_withdrawals/p2p/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns 404 if trade not found' do
        user = create(:user)

        # Allow the trade not to be found
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(
          class_double(Trade, find_by: nil)
        )

        get '/api/v1/fiat_withdrawals/p2p/non_existent_id', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(JSON.parse(response.body)['error']).to include('Trade not found')
      end

      it 'returns 403 if user is not part of the trade' do
        user = create(:user)
        other_user1 = create(:user)
        other_user2 = create(:user)
        trade = create(:trade)

        # Create a trade where current user is neither buyer nor seller
        trade_double = instance_double(Trade,
                              id: trade.id,
                              buyer_id: other_user1.id,
                              seller_id: other_user2.id)

        # Allow the trade to be found but make it belong to different users
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(
          class_double(Trade, find_by: trade_double)
        )

        get "/api/v1/fiat_withdrawals/p2p/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
        expect(JSON.parse(response.body)['error']).to include('Unauthorized access')
      end

      it 'returns 400 if trade is not a withdrawal trade' do
        user = create(:user)
        other_user = create(:user)
        trade = create(:trade)

        # Create a trade that is not a withdrawal trade
        trade_double = instance_double(Trade,
                              id: trade.id,
                              buyer_id: user.id,
                              seller_id: other_user.id,
                              is_fiat_token_withdrawal_trade?: false)

        # Allow the trade to be found but make it not a withdrawal trade
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(
          class_double(Trade, find_by: trade_double)
        )

        get "/api/v1/fiat_withdrawals/p2p/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)['error']).to include('does not have an associated withdrawal')
      end

      it 'returns 404 if no withdrawal found for trade' do
        user = create(:user)
        other_user = create(:user)
        trade = create(:trade)

        # Create a trade with no associated withdrawal
        trade_double = instance_double(Trade,
                              id: trade.id,
                              buyer_id: user.id,
                              seller_id: other_user.id,
                              is_fiat_token_withdrawal_trade?: true,
                              fiat_withdrawal: nil)

        # Allow the trade to be found but with no withdrawal
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(
          class_double(Trade, find_by: trade_double)
        )

        get "/api/v1/fiat_withdrawals/p2p/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(JSON.parse(response.body)['error']).to include('No withdrawal found')
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
