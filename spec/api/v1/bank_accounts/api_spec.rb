# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::BankAccounts::Api, type: :request do
  let!(:user) { create(:user) }

  describe 'GET /api/v1/bank_accounts' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        get '/api/v1/bank_accounts'

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'returns all bank accounts of the user' do
        bank_account1 = create(:bank_account, user: user, is_primary: true, created_at: 1.day.ago)
        bank_account2 = create(:bank_account, user: user, created_at: Time.zone.today)
        # Create another user's bank account to ensure it's not returned
        create(:bank_account)

        get '/api/v1/bank_accounts', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(2)
        expect(json_response.map { |ba| ba['id'] }).to contain_exactly(bank_account1.id, bank_account2.id)
        # Primary account should be first
        expect(json_response.first['id']).to eq(bank_account1.id)
      end

      it 'filters bank accounts by country_code' do
        vietnam_account = create(:bank_account, :vietnam, user: user)
        philippines_account = create(:bank_account, :philippines, user: user)

        get '/api/v1/bank_accounts', params: { country_code: 'vn' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
        expect(json_response.first['id']).to eq(vietnam_account.id)
        expect(json_response.first['country_code']).to eq('vn')
      end

      it 'filters bank accounts by bank_name' do
        account1 = create(:bank_account, user: user, bank_name: 'Vietcombank')
        account2 = create(:bank_account, user: user, bank_name: 'BIDV')

        get '/api/v1/bank_accounts', params: { bank_name: 'Vietcombank' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
        expect(json_response.first['id']).to eq(account1.id)
        expect(json_response.first['bank_name']).to eq('Vietcombank')
      end

      it 'filters bank accounts by verification status' do
        verified_account = create(:bank_account, :verified, user: user)
        unverified_account = create(:bank_account, user: user)

        get '/api/v1/bank_accounts', params: { verified: true }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
        expect(json_response.first['id']).to eq(verified_account.id)
        expect(json_response.first['verified']).to be(true)
      end

      it 'paginates results' do
        create_list(:bank_account, 3, user: user)

        get '/api/v1/bank_accounts', params: { page: 1, per_page: 2 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(2)

        get '/api/v1/bank_accounts', params: { page: 2, per_page: 2 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
      end
    end
  end

  describe 'GET /api/v1/bank_accounts/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        bank_account = create(:bank_account)

        get "/api/v1/bank_accounts/#{bank_account.id}"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'returns the bank account details' do
        bank_account = create(:bank_account,
                             user: user,
                             bank_name: 'Vietcombank',
                             account_name: 'John Doe',
                             account_number: '1234567890',
                             branch: 'Hanoi Branch',
                             country_code: 'vn',
                             verified: true,
                             is_primary: true)

        get "/api/v1/bank_accounts/#{bank_account.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['id']).to eq(bank_account.id)
        expect(json_response['bank_name']).to eq('Vietcombank')
        expect(json_response['account_name']).to eq('John Doe')
        expect(json_response['account_number']).to eq('1234567890')
        expect(json_response['branch']).to eq('Hanoi Branch')
        expect(json_response['country_code']).to eq('vn')
        expect(json_response['verified']).to be(true)
        expect(json_response['is_primary']).to be(true)
        expect(json_response['user_id']).to eq(user.id)
        expect(json_response).to have_key('created_at')
        expect(json_response).to have_key('updated_at')
      end

      it 'returns 404 when bank account is not found' do
        get '/api/v1/bank_accounts/non_existent_id', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 404 when bank account belongs to another user' do
        another_user = create(:user)
        bank_account = create(:bank_account, user: another_user)

        get "/api/v1/bank_accounts/#{bank_account.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'POST /api/v1/bank_accounts' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        post '/api/v1/bank_accounts', params: { bank_name: 'Test Bank' }

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'creates a new bank account with valid params' do
        # Create a bank account first so the next one isn't primary by default
        create(:bank_account, user: user)

        params = {
          bank_name: 'Vietcombank',
          account_name: 'John Doe',
          account_number: '1234567890',
          branch: 'Hanoi Branch',
          country_code: 'vn',
          is_primary: false
        }

        expect {
          post '/api/v1/bank_accounts', params: params, headers: auth_headers(user)
        }.to change(BankAccount, :count).by(1)

        expect(response).to have_http_status(:created)
        expect(json_response['bank_name']).to eq('Vietcombank')
        expect(json_response['account_name']).to eq('John Doe')
        expect(json_response['account_number']).to eq('1234567890')
        expect(json_response['branch']).to eq('Hanoi Branch')
        expect(json_response['country_code']).to eq('vn')
        expect(json_response['verified']).to be(false)
        expect(json_response['is_primary']).to be(false)
        expect(json_response['user_id']).to eq(user.id)
      end

      it 'sets is_primary to true for the first bank account' do
        params = {
          bank_name: 'Vietcombank',
          account_name: 'John Doe',
          account_number: '1234567890',
          branch: 'Hanoi Branch',
          country_code: 'vn'
        }

        post '/api/v1/bank_accounts', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:created)
        expect(json_response['is_primary']).to be(true)
      end

      it 'returns 400 when bank account limit is reached' do
        # Create max accounts
        create_list(:bank_account, 10, user: user)

        params = {
          bank_name: 'Vietcombank',
          account_name: 'John Doe',
          account_number: '1234567890',
          branch: 'Hanoi Branch',
          country_code: 'vn'
        }

        post '/api/v1/bank_accounts', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('You can have at most 10 bank accounts')
      end

      it 'returns 422 with invalid params' do
        params = {
          bank_name: '',
          account_name: 'John Doe',
          account_number: '1234567890',
          country_code: 'vn'
        }

        post '/api/v1/bank_accounts', params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to include("Bank name can't be blank")
      end
    end
  end

  describe 'PUT /api/v1/bank_accounts/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        bank_account = create(:bank_account)

        put "/api/v1/bank_accounts/#{bank_account.id}", params: { bank_name: 'New Bank' }

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'updates the bank account with valid params' do
        bank_account = create(:bank_account, user: user, bank_name: 'Old Bank')

        params = { bank_name: 'New Bank', branch: 'New Branch' }

        put "/api/v1/bank_accounts/#{bank_account.id}", params: params, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['bank_name']).to eq('New Bank')
        expect(json_response['branch']).to eq('New Branch')
      end

      it 'updates is_primary status' do
        bank_account = create(:bank_account, user: user, is_primary: false)

        put "/api/v1/bank_accounts/#{bank_account.id}", params: { is_primary: true }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['is_primary']).to be(true)
      end

      it 'returns 400 when attempting to update information on a verified account' do
        bank_account = create(:bank_account, :verified, user: user)

        put "/api/v1/bank_accounts/#{bank_account.id}",
            params: { bank_name: 'New Bank' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Cannot change information of a verified account')
      end

      it 'allows updating branch on a verified account' do
        bank_account = create(:bank_account, :verified, user: user, branch: 'Old Branch')

        put "/api/v1/bank_accounts/#{bank_account.id}",
            params: { branch: 'New Branch' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['branch']).to eq('New Branch')
      end

      it 'allows updating is_primary on a verified account' do
        bank_account = create(:bank_account, :verified, user: user, is_primary: false)

        put "/api/v1/bank_accounts/#{bank_account.id}",
            params: { is_primary: true },
            headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['is_primary']).to be(true)
      end

      it 'returns 404 when bank account is not found' do
        put '/api/v1/bank_accounts/non_existent_id',
            params: { bank_name: 'New Bank' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 422 with invalid params' do
        bank_account = create(:bank_account, user: user)

        # Set up test to fail validation
        allow_any_instance_of(BankAccount).to receive(:update).and_return(false)
        full_messages = instance_double(Array, join: 'Error message')
        errors = instance_double(ActiveModel::Errors, full_messages: full_messages)
        allow_any_instance_of(BankAccount).to receive(:errors).and_return(errors)

        put "/api/v1/bank_accounts/#{bank_account.id}",
            params: { bank_name: 'New Name' },
            headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Error message')
      end
    end
  end

  describe 'DELETE /api/v1/bank_accounts/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        bank_account = create(:bank_account)

        delete "/api/v1/bank_accounts/#{bank_account.id}"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'deletes the bank account' do
        bank_account = create(:bank_account, user: user, is_primary: false)

        expect {
          delete "/api/v1/bank_accounts/#{bank_account.id}", headers: auth_headers(user)
        }.to change(BankAccount, :count).by(-1)

        expect(response).to have_http_status(:ok)
        expect(json_response['success']).to be(true)
        expect(json_response['message']).to eq('Bank account has been deleted')
      end

      it 'returns 400 when deleting the only bank account' do
        bank_account = create(:bank_account, user: user, is_primary: true)

        delete "/api/v1/bank_accounts/#{bank_account.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Cannot delete the only bank account')
      end

      it 'returns 400 when deleting a primary account with multiple accounts' do
        create(:bank_account, user: user, is_primary: false)
        primary_account = create(:bank_account, user: user, is_primary: true)

        delete "/api/v1/bank_accounts/#{primary_account.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Cannot delete primary account. Please set another account as primary first')
      end

      it 'returns 404 when bank account is not found' do
        delete '/api/v1/bank_accounts/non_existent_id', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 422 when bank account cannot be deleted' do
        bank_account = create(:bank_account, user: user, is_primary: false)

        allow_any_instance_of(BankAccount).to receive(:destroy).and_return(false)

        delete "/api/v1/bank_accounts/#{bank_account.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Could not delete bank account')
      end
    end
  end

  describe 'PUT /api/v1/bank_accounts/:id/primary' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        bank_account = create(:bank_account)

        put "/api/v1/bank_accounts/#{bank_account.id}/primary"

        expect(response).to have_http_status(:unauthorized)
        expect(JSON.parse(response.body)).to eq(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when user is authenticated' do
      it 'sets the bank account as primary' do
        bank_account = create(:bank_account, user: user, is_primary: false)

        put "/api/v1/bank_accounts/#{bank_account.id}/primary", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['is_primary']).to be(true)
      end

      it 'returns 404 when bank account is not found' do
        put '/api/v1/bank_accounts/non_existent_id/primary', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 422 when bank account cannot be set as primary' do
        bank_account = create(:bank_account, user: user, is_primary: false)

        allow_any_instance_of(BankAccount).to receive(:mark_as_primary!).and_return(false)

        put "/api/v1/bank_accounts/#{bank_account.id}/primary", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Could not set account as primary')
      end
    end
  end
end
