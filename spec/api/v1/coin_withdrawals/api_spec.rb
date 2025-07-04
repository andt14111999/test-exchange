# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::CoinWithdrawals::Api, sidekiq: :inline, type: :request do
  include ApiHelpers

  let(:user) { create(:user) }
  let(:other_user) { create(:user) }
  let(:coin_currency) { 'usdt' }
  let(:coin_layer) { 'erc20' }
  let(:coin_amount) { 50.0 }
  let(:coin_address) { '0x1234567890abcdef1234567890abcdef12345678' }
  let(:main_coin_account) { create(:coin_account, :usdt_main, user: user, balance: 200.0) }
  let(:deposit_coin_account) { create(:coin_account, :usdt_erc20, user: user) }
  let(:other_user_main_account) { create(:coin_account, :usdt_main, user: other_user, balance: 200.0) }
  let(:auth_header) { auth_headers(user) }
  let(:device_uuid) { SecureRandom.uuid }
  let(:valid_params) do
    {
      coin_address: '0x1234567890abcdef1234567890abcdef12345678',
      coin_amount: coin_amount,
      coin_currency: coin_currency,
      coin_layer: coin_layer
    }
  end

  before do
    # Ensure the coin accounts exist
    main_coin_account
    deposit_coin_account
    other_user_main_account
  end

  describe 'POST /api/v1/coin_withdrawals' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/coin_withdrawals', params: valid_params
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'with valid parameters' do
        it 'creates a new withdrawal' do
          # Enable 2FA for user and create trusted device
          user.update!(authenticator_enabled: true)
          create(:access_device, :trusted, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
          headers = auth_header.merge('Device-Uuid' => device_uuid)

          expect {
            post '/api/v1/coin_withdrawals', params: valid_params, headers: headers
          }.to change(CoinWithdrawal, :count).by(1)

          expect(response).to have_http_status(:success)

          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
          expect(json_response['data']['coin_currency']).to eq(coin_currency)
          expect(json_response['data']['coin_amount'].to_f).to eq(coin_amount)
          expect(json_response['data']['coin_address']).to eq(coin_address)
          expect(json_response['data']['coin_layer']).to eq(coin_layer)
          expect(json_response['data']['status']).to eq('pending')
        end

        it 'creates a new withdrawal with API key authentication' do
          # Enable 2FA for user and create trusted device
          user.update!(authenticator_enabled: true)
          create(:access_device, :trusted, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))

          api_key = create(:api_key, user: user)
          timestamp = Time.current.to_i.to_s
          path = '/api/v1/coin_withdrawals'
          method = 'POST'
          message = "#{method}#{path}#{timestamp}"

          # Generate HMAC signature
          digest = OpenSSL::Digest.new('sha256')
          signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

          headers = {
            'X-Access-Key' => api_key.access_key,
            'X-Signature' => signature,
            'X-Timestamp' => timestamp,
            'Device-Uuid' => device_uuid
          }

          expect {
            post '/api/v1/coin_withdrawals', params: valid_params, headers: headers
          }.to change(CoinWithdrawal, :count).by(1)

          expect(response).to have_http_status(:success)

          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
          expect(json_response['data']['coin_currency']).to eq(coin_currency)
          expect(json_response['data']['coin_amount'].to_f).to eq(coin_amount)
          expect(json_response['data']['coin_address']).to eq(coin_address)
          expect(json_response['data']['coin_layer']).to eq(coin_layer)
          expect(json_response['data']['status']).to eq('pending')
        end
      end

      context 'with insufficient balance' do
        let(:coin_amount) { 300.0 }

        it 'returns a validation error' do
          # Mock 2FA check to bypass authentication
          allow_any_instance_of(V1::Helpers::DeviceHelper).to receive(:require_2fa_for_action?).and_return(false)

          post '/api/v1/coin_withdrawals', params: valid_params, headers: auth_header

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('error')
          expect(json_response['message']).to include('Coin amount exceeds available balance')
        end
      end

      context 'when creating a regular coin withdrawal' do
        it 'creates a new coin withdrawal' do
          # Enable 2FA for user and create trusted device
          user.update!(authenticator_enabled: true)
          create(:access_device, :trusted, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))

          params = {
            coin_address: '0x1234567890123456789012345678901234567890',
            coin_amount: 10.0,
            coin_currency: 'usdt',
            coin_layer: 'erc20'
          }
          headers = auth_headers(user).merge('Device-Uuid' => device_uuid)

          post '/api/v1/coin_withdrawals', params: params, headers: headers

          expect(response.status).to eq 201
          expect(JSON.parse(response.body)['status']).to eq 'success'
          expect(JSON.parse(response.body)['data']['coin_amount']).to eq '10.0'
          expect(JSON.parse(response.body)['data']['coin_address']).to eq '0x1234567890123456789012345678901234567890'
          expect(JSON.parse(response.body)['data']['is_internal_transfer']).to be false
        end
      end

      context 'when creating an internal transfer' do
        it 'creates a new internal transfer withdrawal using receiver_email' do
          sender = create(:user)
          receiver = create(:user)
          create(:coin_account, user: sender, coin_currency: 'usdt', layer: 'erc20', balance: 100)

          # Enable 2FA for sender and create trusted device
          sender.update!(authenticator_enabled: true)
          create(:access_device, :trusted, user: sender, device_uuid_hash: AccessDevice.digest(device_uuid))

          # Stub validation to allow the internal transfer to proceed
          allow_any_instance_of(CoinWithdrawal).to receive(:validate_coin_amount).and_return(true)

          params = {
            coin_amount: 10.0,
            coin_currency: 'usdt',
            receiver_email: receiver.email
          }
          headers = auth_headers(sender).merge('Device-Uuid' => device_uuid)

          post '/api/v1/coin_withdrawals', params: params, headers: headers

          expect(response.status).to eq 201
          expect(JSON.parse(response.body)['status']).to eq 'success'
          expect(JSON.parse(response.body)['data']['coin_amount']).to eq '10.0'
          expect(JSON.parse(response.body)['data']['receiver_email']).to eq receiver.email
          expect(JSON.parse(response.body)['data']['is_internal_transfer']).to be true

          # Verify internal transfer operation was created
          withdrawal_id = JSON.parse(response.body)['data']['id']
          withdrawal = CoinWithdrawal.find(withdrawal_id)
          withdrawal.process! # Trigger processing state to create operations
          expect(withdrawal.coin_internal_transfer_operation).to be_present
          expect(withdrawal.coin_internal_transfer_operation.receiver.email).to eq receiver.email
        end

        it 'creates a new internal transfer withdrawal using receiver_username' do
          sender = create(:user)
          receiver = create(:user)
          receiver.update!(username: 'testuser123')
          create(:coin_account, user: sender, coin_currency: 'usdt', layer: 'erc20', balance: 100)

          # Enable 2FA for sender and create trusted device
          sender.update!(authenticator_enabled: true)
          create(:access_device, :trusted, user: sender, device_uuid_hash: AccessDevice.digest(device_uuid))

          # Stub validation to allow the internal transfer to proceed
          allow_any_instance_of(CoinWithdrawal).to receive(:validate_coin_amount).and_return(true)

          params = {
            coin_amount: 10.0,
            coin_currency: 'usdt',
            receiver_username: receiver.username
          }
          headers = auth_headers(sender).merge('Device-Uuid' => device_uuid)

          post '/api/v1/coin_withdrawals', params: params, headers: headers

          expect(response.status).to eq 201
          expect(JSON.parse(response.body)['status']).to eq 'success'
          expect(JSON.parse(response.body)['data']['coin_amount']).to eq '10.0'
          expect(JSON.parse(response.body)['data']['is_internal_transfer']).to be true

          # Verify internal transfer operation was created
          withdrawal_id = JSON.parse(response.body)['data']['id']
          withdrawal = CoinWithdrawal.find(withdrawal_id)
          withdrawal.process! # Trigger processing state to create operations
          expect(withdrawal.coin_internal_transfer_operation).to be_present
          expect(withdrawal.coin_internal_transfer_operation.receiver.username).to eq receiver.username
        end

        it 'validates receiver username existence' do
          user = create(:user)
          create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', balance: 100)

          # Mock 2FA check to bypass authentication
          allow_any_instance_of(V1::Helpers::DeviceHelper).to receive(:require_2fa_for_action?).and_return(false)

          params = {
            coin_amount: 10.0,
            coin_currency: 'usdt',
            receiver_username: 'nonexistent_username'
          }

          post '/api/v1/coin_withdrawals', params: params, headers: auth_headers(user)

          expect(response.status).to eq 422
          expect(JSON.parse(response.body)['status']).to eq 'error'
          expect(JSON.parse(response.body)['message']).to include('Receiver username not found')
        end

        it 'validates receiver email existence' do
          user = create(:user)
          create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', balance: 100)

          # Mock 2FA check to bypass authentication
          allow_any_instance_of(V1::Helpers::DeviceHelper).to receive(:require_2fa_for_action?).and_return(false)

          params = {
            coin_amount: 10.0,
            coin_currency: 'usdt',
            receiver_email: 'nonexistent@example.com'
          }

          post '/api/v1/coin_withdrawals', params: params, headers: auth_headers(user)

          expect(response.status).to eq 422
          expect(JSON.parse(response.body)['status']).to eq 'error'
          expect(JSON.parse(response.body)['message']).to include('Receiver email not found')
        end

        it 'prevents transferring to self via email' do
          user = create(:user)
          create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', balance: 1000)

          # Mock 2FA check to bypass authentication
          allow_any_instance_of(V1::Helpers::DeviceHelper).to receive(:require_2fa_for_action?).and_return(false)

          # Stub the validation for this specific test because it will fail otherwise
          allow_any_instance_of(CoinWithdrawal).to receive(:validate_coin_amount).and_return(true)

          params = {
            coin_amount: 10.0,
            coin_currency: 'usdt',
            receiver_email: user.email
          }

          post '/api/v1/coin_withdrawals', params: params, headers: auth_headers(user)

          expect(response.status).to eq 422
          expect(JSON.parse(response.body)['status']).to eq 'error'
          expect(JSON.parse(response.body)['message']).to include('cannot transfer to self')
        end

        it 'prevents transferring to self via username' do
          user = create(:user)
          user.update!(username: 'myusername')
          create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', balance: 1000)

          # Mock 2FA check to bypass authentication
          allow_any_instance_of(V1::Helpers::DeviceHelper).to receive(:require_2fa_for_action?).and_return(false)

          # Stub the validation for this specific test because it will fail otherwise
          allow_any_instance_of(CoinWithdrawal).to receive(:validate_coin_amount).and_return(true)

          params = {
            coin_amount: 10.0,
            coin_currency: 'usdt',
            receiver_username: user.username
          }

          post '/api/v1/coin_withdrawals', params: params, headers: auth_headers(user)

          expect(response.status).to eq 422
          expect(JSON.parse(response.body)['status']).to eq 'error'
          expect(JSON.parse(response.body)['message']).to match(/cannot_transfer_to_self/)
        end
      end

      context 'when user has 2FA disabled' do
        it 'requires 2FA code even when user has no 2FA enabled' do
          user_no_2fa = create(:user)
          create(:coin_account, :usdt_main, user: user_no_2fa, balance: 200.0)

          withdrawal_params = {
            coin_address: coin_address,
            coin_amount: coin_amount,
            coin_currency: coin_currency,
            coin_layer: coin_layer
          }

          post '/api/v1/coin_withdrawals', params: withdrawal_params, headers: auth_headers(user_no_2fa)

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('2FA code is required for this action')
          expect(json_response['requires_2fa']).to be true
          expect(json_response['device_trusted']).to be false
        end
      end

      context 'when user has 2FA enabled without trusted device' do
        it 'requires 2FA code' do
          user_with_2fa = create(:user, :with_2fa)
          create(:coin_account, :usdt_main, user: user_with_2fa, balance: 200.0)

          withdrawal_params = {
            coin_address: coin_address,
            coin_amount: coin_amount,
            coin_currency: coin_currency,
            coin_layer: coin_layer
          }

          post '/api/v1/coin_withdrawals', params: withdrawal_params, headers: auth_headers(user_with_2fa)

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('2FA code is required for this action')
          expect(json_response['requires_2fa']).to be true
          expect(json_response['device_trusted']).to be false
        end

        it 'rejects invalid 2FA code' do
          user_with_2fa = create(:user, :with_2fa)
          create(:coin_account, :usdt_main, user: user_with_2fa, balance: 200.0)

          withdrawal_params = {
            coin_address: coin_address,
            coin_amount: coin_amount,
            coin_currency: coin_currency,
            coin_layer: coin_layer,
            two_factor_code: '000000'
          }

          post '/api/v1/coin_withdrawals', params: withdrawal_params, headers: auth_headers(user_with_2fa)

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('Invalid 2FA code')
        end

        it 'accepts valid 2FA code and creates withdrawal' do
          user_with_2fa = create(:user, :with_2fa)
          create(:coin_account, :usdt_main, user: user_with_2fa, balance: 200.0)
          allow_any_instance_of(User).to receive(:verify_otp).and_return(true)

          withdrawal_params = {
            coin_address: coin_address,
            coin_amount: coin_amount,
            coin_currency: coin_currency,
            coin_layer: coin_layer,
            two_factor_code: '123456'
          }
          headers = auth_headers(user_with_2fa).merge('Device-Uuid' => device_uuid)

          post '/api/v1/coin_withdrawals', params: withdrawal_params, headers: headers

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')

          # Should create access device after successful 2FA
          expect(user_with_2fa.access_devices.count).to eq(1)
        end
      end

      context 'when user has 2FA enabled with trusted device' do
        it 'allows withdrawal without 2FA code for first device' do
          user_with_2fa = create(:user, :with_2fa)
          create(:coin_account, :usdt_main, user: user_with_2fa, balance: 200.0)
          create(:access_device, :trusted, user: user_with_2fa, device_uuid_hash: AccessDevice.digest(device_uuid), first_device: true)

          withdrawal_params = {
            coin_address: coin_address,
            coin_amount: coin_amount,
            coin_currency: coin_currency,
            coin_layer: coin_layer
          }
          headers = auth_headers(user_with_2fa).merge('Device-Uuid' => device_uuid)

          post '/api/v1/coin_withdrawals', params: withdrawal_params, headers: headers

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
        end

        it 'allows withdrawal without 2FA code for aged device (72+ hours)' do
          user_with_2fa = create(:user, :with_2fa)
          create(:coin_account, :usdt_main, user: user_with_2fa, balance: 200.0)
          create(:access_device, :aged_trusted, user: user_with_2fa, device_uuid_hash: AccessDevice.digest(device_uuid))

          withdrawal_params = {
            coin_address: coin_address,
            coin_amount: coin_amount,
            coin_currency: coin_currency,
            coin_layer: coin_layer
          }
          headers = auth_headers(user_with_2fa).merge('Device-Uuid' => device_uuid)

          post '/api/v1/coin_withdrawals', params: withdrawal_params, headers: headers

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
        end
      end
    end
  end

  describe 'GET /api/v1/coin_withdrawals/check_receiver' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/coin_withdrawals/check_receiver', params: { receiver_username: 'testuser' }
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'when receiver_username parameter is missing' do
        it 'returns bad request error' do
          get '/api/v1/coin_withdrawals/check_receiver', headers: auth_header

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['error']).to include('receiver_username is missing')
        end
      end

            context 'when receiver exists' do
        it 'returns true for existing username' do
          existing_user = create(:user, username: 'existinguser')

          get '/api/v1/coin_withdrawals/check_receiver',
               params: { receiver_username: existing_user.username },
               headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response).to be true
        end
      end

      context 'when receiver does not exist' do
        it 'returns false for non-existing username' do
          get '/api/v1/coin_withdrawals/check_receiver',
               params: { receiver_username: 'nonexistentuser' },
               headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response).to be false
        end
      end

      context 'when receiver_username is empty string' do
        it 'returns false for empty username' do
          get '/api/v1/coin_withdrawals/check_receiver',
               params: { receiver_username: '' },
               headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response).to be false
        end
      end

      context 'when receiver_username contains special characters' do
        it 'returns false for username with special characters' do
          get '/api/v1/coin_withdrawals/check_receiver',
               params: { receiver_username: 'user@#$%' },
               headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response).to be false
        end
      end

      context 'when checking current user username' do
        it 'returns false when checking own username' do
          user.update!(username: 'myownusername')

          get '/api/v1/coin_withdrawals/check_receiver',
               params: { receiver_username: user.username },
               headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response).to be false
        end
      end

      context 'with API key authentication' do
        it 'returns receiver existence status with valid API key' do
          existing_user = create(:user, username: 'apiuser')
          api_key = create(:api_key, user: user)
          timestamp = Time.current.to_i.to_s
          path = '/api/v1/coin_withdrawals/check_receiver'
          method = 'GET'
          message = "#{method}#{path}#{timestamp}"

          # Generate HMAC signature
          digest = OpenSSL::Digest.new('sha256')
          signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

          headers = {
            'X-Access-Key' => api_key.access_key,
            'X-Signature' => signature,
            'X-Timestamp' => timestamp
          }

          get '/api/v1/coin_withdrawals/check_receiver',
               params: { receiver_username: existing_user.username },
               headers: headers

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response).to be true
        end
      end
    end
  end

  describe 'GET /api/v1/coin_withdrawals/:id' do
    let!(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: coin_currency, coin_amount: coin_amount, coin_address: coin_address, coin_layer: coin_layer) }
    let!(:other_user_withdrawal) { create(:coin_withdrawal, user: other_user, coin_currency: coin_currency, coin_amount: coin_amount, coin_address: coin_address, coin_layer: coin_layer) }

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get "/api/v1/coin_withdrawals/#{withdrawal.id}"
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'when withdrawal exists and belongs to user' do
        it 'returns the withdrawal details' do
          get "/api/v1/coin_withdrawals/#{withdrawal.id}", headers: auth_header

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
          expect(json_response['data']['id']).to eq(withdrawal.id)
          expect(json_response['data']['coin_currency']).to eq(withdrawal.coin_currency)
          expect(json_response['data']['coin_amount']).to eq(withdrawal.coin_amount.to_s)
          expect(json_response['data']['coin_fee']).to eq(withdrawal.coin_fee.to_s)
          expect(json_response['data']['coin_address']).to eq(withdrawal.coin_address)
          expect(json_response['data']['coin_layer']).to eq(withdrawal.coin_layer)
          expect(json_response['data']['status']).to eq(withdrawal.status)
        end

        it 'returns the withdrawal details using API key authentication' do
          api_key = create(:api_key, user: user)
          timestamp = Time.current.to_i.to_s
          path = "/api/v1/coin_withdrawals/#{withdrawal.id}"
          method = 'GET'
          message = "#{method}#{path}#{timestamp}"

          # Generate HMAC signature
          digest = OpenSSL::Digest.new('sha256')
          signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

          headers = {
            'X-Access-Key' => api_key.access_key,
            'X-Signature' => signature,
            'X-Timestamp' => timestamp
          }

          get "/api/v1/coin_withdrawals/#{withdrawal.id}", headers: headers

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('success')
          expect(json_response['data']['id']).to eq(withdrawal.id)
          expect(json_response['data']['coin_currency']).to eq(withdrawal.coin_currency)
          expect(json_response['data']['coin_amount']).to eq(withdrawal.coin_amount.to_s)
          expect(json_response['data']['coin_fee']).to eq(withdrawal.coin_fee.to_s)
          expect(json_response['data']['coin_address']).to eq(withdrawal.coin_address)
          expect(json_response['data']['coin_layer']).to eq(withdrawal.coin_layer)
          expect(json_response['data']['status']).to eq(withdrawal.status)
        end
      end

      context 'when withdrawal does not exist' do
        it 'returns not found error' do
          get "/api/v1/coin_withdrawals/999999", headers: auth_header

          expect(response).to have_http_status(:not_found)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('error')
          expect(json_response['message']).to eq('Coin withdrawal not found')
        end
      end

      context 'when withdrawal belongs to another user' do
        it 'returns not found error' do
          get "/api/v1/coin_withdrawals/#{other_user_withdrawal.id}", headers: auth_header

          expect(response).to have_http_status(:not_found)
          json_response = JSON.parse(response.body)
          expect(json_response['status']).to eq('error')
          expect(json_response['message']).to eq('Coin withdrawal not found')
        end
      end

      context 'when retrieving a regular withdrawal' do
        it 'returns withdrawal details' do
          get "/api/v1/coin_withdrawals/#{withdrawal.id}", headers: auth_headers(user)

          expect(response.status).to eq 200
          expect(JSON.parse(response.body)['status']).to eq 'success'
          expect(JSON.parse(response.body)['data']['id']).to eq withdrawal.id
          expect(JSON.parse(response.body)['data']['coin_address']).to eq withdrawal.coin_address
          expect(JSON.parse(response.body)['data']['is_internal_transfer']).to be false
        end
      end

      context 'when retrieving an internal transfer' do
        it 'returns internal transfer details' do
          sender = create(:user)
          receiver = create(:user)

          # Create sender's account with large balance
          _coin_account = create(:coin_account, :usdt_main,
            user: sender,
            balance: 1000,
            frozen_balance: 0
          )

          withdrawal = create(:coin_withdrawal, :internal,
            user: sender,
            coin_currency: 'usdt',
            coin_amount: 30.0,
            status: 'pending',
            receiver_email: receiver.email
          )
          withdrawal.process! # Trigger processing state to create operations

          get "/api/v1/coin_withdrawals/#{withdrawal.id}", headers: auth_headers(sender)

          expect(response.status).to eq 200
          expect(JSON.parse(response.body)['status']).to eq 'success'
          expect(JSON.parse(response.body)['data']['id']).to eq withdrawal.id
          expect(JSON.parse(response.body)['data']['is_internal_transfer']).to be true
          expect(JSON.parse(response.body)['data']['receiver_email']).to eq receiver.email
          expect(JSON.parse(response.body)['data']['internal_transfer_status']).to eq withdrawal.coin_internal_transfer_operation.status
          expect(JSON.parse(response.body)['data']).not_to have_key('coin_address')
        end
      end
    end
  end
end
