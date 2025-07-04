require 'rails_helper'

RSpec.describe V1::Users::Api, type: :request do
  let(:user) { create(:user) }

  describe 'GET /api/v1/users/me' do
    context 'when user is authenticated' do
      it 'returns current user information' do
        get '/api/v1/users/me', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response).to include(
          'id' => user.id,
          'email' => user.email,
          'username' => user.username,
          'display_name' => user.display_name,
          'avatar_url' => user.avatar_url,
          'role' => user.role,
          'status' => user.status,
          'kyc_level' => user.kyc_level,
          'phone_verified' => user.phone_verified,
          'document_verified' => user.document_verified,
          'authenticator_enabled' => user.authenticator_enabled
        )
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/users/me'

        expect(response).to have_http_status(:unauthorized)
        expect(json_response).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end

    context 'when token is invalid' do
      it 'returns unauthorized error' do
        get '/api/v1/users/me', headers: { 'Authorization' => 'Bearer invalid_token' }

        expect(response).to have_http_status(:unauthorized)
        expect(json_response).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end
  end

  describe 'PATCH /api/v1/users/username' do
    context 'when user is authenticated' do
      context 'when username is not set yet' do
        it 'updates the username' do
          patch '/api/v1/users/username', params: { username: 'newusername' }, headers: auth_headers(user)

          expect(response).to have_http_status(:ok)
          expect(json_response).to include('username' => 'newusername')
          expect(user.reload.username).to eq('newusername')
        end
      end

      context 'when username is already set' do
        it 'returns error' do
          user.update(username: 'existingusername')
          patch '/api/v1/users/username', params: { username: 'newusername' }, headers: auth_headers(user)

          expect(response).to have_http_status(:unprocessable_entity)
          expect(json_response['errors']).to include('Username cannot be changed once set')
        end
      end

      context 'when username is already taken' do
        it 'returns error' do
          other_user = create(:user, username: 'takenusername')
          patch '/api/v1/users/username', params: { username: 'takenusername' }, headers: auth_headers(user)

          expect(response).to have_http_status(:unprocessable_entity)
          expect(json_response['errors']).to include('Username has already been taken')
        end
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        patch '/api/v1/users/username', params: { username: 'newusername' }

        expect(response).to have_http_status(:unauthorized)
        expect(json_response).to include(
          'status' => 'error',
          'message' => 'Unauthorized'
        )
      end
    end
  end

  describe '2FA endpoints' do
    describe 'POST /api/v1/users/two_factor_auth/enable' do
      context 'when user is authenticated and 2FA is not enabled' do
        it 'generates QR code URI' do
          user.update(username: 'testuser')
          post '/api/v1/users/two_factor_auth/enable', headers: auth_headers(user)

          expect(response).to have_http_status(:ok)
          expect(json_response).to include(
            'qr_code_uri' => a_string_including('otpauth://totp/Snowfox%20Exchange:testuser'),
            'message' => 'Scan the QR code with your authenticator app, then verify with a code'
          )
          expect(user.reload.authenticator_key).to be_present
          expect(user.authenticator_enabled).to be false
        end
      end

      context 'when user is authenticated and 2FA is already enabled' do
        it 'returns error message' do
          user.update(authenticator_enabled: true)
          post '/api/v1/users/two_factor_auth/enable', headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          expect(json_response).to include('message' => '2FA is already enabled')
        end
      end

      context 'when user is not authenticated' do
        it 'returns unauthorized error' do
          post '/api/v1/users/two_factor_auth/enable'

          expect(response).to have_http_status(:unauthorized)
        end
      end
    end

    describe 'POST /api/v1/users/two_factor_auth/verify' do
      context 'when user is authenticated and provides valid code' do
        it 'enables 2FA successfully' do
          user.assign_authenticator_key
          user.save
          totp = ROTP::TOTP.new(user.authenticator_key)
          valid_code = totp.now
          device_uuid = SecureRandom.uuid
          device_headers = {
            'Device-Uuid' => device_uuid,
            'Device-Type' => 'web',
            'Browser' => 'Chrome',
            'Os' => 'macOS'
          }

          post '/api/v1/users/two_factor_auth/verify',
               params: { code: valid_code },
               headers: auth_headers(user).merge(device_headers)

          expect(response).to have_http_status(:ok)
          expect(json_response).to include('message' => '2FA has been successfully enabled')
          expect(user.reload.authenticator_enabled).to be true

          # Check that device is created but not automatically trusted
          device = user.access_devices.first
          expect(device).to be_present
          expect(device.trusted).to be false
          expect(device.first_device).to be true
        end

        it 'enables 2FA and marks device as trusted when Device-Trusted header is true' do
          user.assign_authenticator_key
          user.save
          totp = ROTP::TOTP.new(user.authenticator_key)
          valid_code = totp.now
          device_uuid = SecureRandom.uuid
          device_headers = {
            'Device-Uuid' => device_uuid,
            'Device-Type' => 'web',
            'Browser' => 'Chrome',
            'Os' => 'macOS',
            'Device-Trusted' => 'true'
          }

          post '/api/v1/users/two_factor_auth/verify',
               params: { code: valid_code },
               headers: auth_headers(user).merge(device_headers)

          expect(response).to have_http_status(:ok)
          expect(json_response).to include('message' => '2FA has been successfully enabled')
          expect(user.reload.authenticator_enabled).to be true

          # Check that device is created and marked as trusted due to header
          device = user.access_devices.first
          expect(device).to be_present
          expect(device.trusted).to be true
          expect(device.first_device).to be true
        end
      end

      context 'when user is authenticated and provides invalid code' do
        it 'returns invalid code error' do
          user.assign_authenticator_key
          user.save

          post '/api/v1/users/two_factor_auth/verify',
               params: { code: '000000' },
               headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          expect(json_response).to include('message' => 'Invalid verification code')
          expect(user.reload.authenticator_enabled).to be false
        end
      end

      context 'when user is authenticated and 2FA is already enabled' do
        it 'returns already enabled error' do
          user.update(authenticator_enabled: true)
          post '/api/v1/users/two_factor_auth/verify',
               params: { code: '123456' },
               headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          expect(json_response).to include('message' => '2FA is already enabled')
        end
      end

      context 'when user is authenticated but 2FA setup not started' do
        it 'returns setup not started error' do
          post '/api/v1/users/two_factor_auth/verify',
               params: { code: '123456' },
               headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          expect(json_response).to include('message' => 'Please re-enable 2FA first')
        end
      end

      context 'when user is not authenticated' do
        it 'returns unauthorized error' do
          post '/api/v1/users/two_factor_auth/verify', params: { code: '123456' }

          expect(response).to have_http_status(:unauthorized)
        end
      end
    end

    describe 'DELETE /api/v1/users/two_factor_auth/disable' do
      context 'when user is authenticated and provides valid code' do
        it 'disables 2FA successfully' do
          user.assign_authenticator_key
          user.update(authenticator_enabled: true)
          totp = ROTP::TOTP.new(user.authenticator_key)
          valid_code = totp.now

          delete '/api/v1/users/two_factor_auth/disable',
                 params: { code: valid_code },
                 headers: auth_headers(user)

          expect(response).to have_http_status(:ok)
          expect(json_response).to include('message' => '2FA has been successfully disabled')
          expect(user.reload.authenticator_enabled).to be false
          expect(user.authenticator_key).to be_nil
        end

        it 'resets all device trusted status to false when disabling 2FA' do
          user.assign_authenticator_key
          user.update(authenticator_enabled: true)

          # Create trusted devices
          device1 = create(:access_device, :trusted, user: user)
          device2 = create(:access_device, :trusted, user: user)
          device3 = create(:access_device, user: user, trusted: false)

          # Store original updated_at times
          original_updated_at_1 = device1.updated_at
          original_updated_at_2 = device2.updated_at
          original_updated_at_3 = device3.updated_at

          # Sleep to ensure time difference
          sleep(0.1)

          totp = ROTP::TOTP.new(user.authenticator_key)
          valid_code = totp.now

          delete '/api/v1/users/two_factor_auth/disable',
                 params: { code: valid_code },
                 headers: auth_headers(user)

          expect(response).to have_http_status(:ok)
          expect(json_response).to include('message' => '2FA has been successfully disabled')

          # Check all devices are now untrusted
          expect(device1.reload.trusted).to be false
          expect(device2.reload.trusted).to be false
          expect(device3.reload.trusted).to be false

          # Check updated_at timestamps were updated
          expect(device1.updated_at).to be > original_updated_at_1
          expect(device2.updated_at).to be > original_updated_at_2
          expect(device3.updated_at).to be > original_updated_at_3
        end
      end

      context 'when user is authenticated and provides invalid code' do
        it 'returns invalid code error' do
          user.assign_authenticator_key
          user.update(authenticator_enabled: true)

          delete '/api/v1/users/two_factor_auth/disable',
                 params: { code: '000000' },
                 headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          expect(json_response).to include('message' => 'Invalid verification code')
          expect(user.reload.authenticator_enabled).to be true
        end
      end

      context 'when user is authenticated but 2FA is not enabled' do
        it 'returns not enabled error' do
          delete '/api/v1/users/two_factor_auth/disable',
                 params: { code: '123456' },
                 headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          expect(json_response).to include('message' => '2FA is not enabled')
        end
      end

      context 'when user is not authenticated' do
        it 'returns unauthorized error' do
          delete '/api/v1/users/two_factor_auth/disable', params: { code: '123456' }

          expect(response).to have_http_status(:unauthorized)
        end
      end
    end
  end
end
