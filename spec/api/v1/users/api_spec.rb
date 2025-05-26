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
          'document_verified' => user.document_verified
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
end
