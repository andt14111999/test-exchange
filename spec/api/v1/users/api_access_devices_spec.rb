# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Users::Api, type: :request do
  include ApiHelpers

  let(:user) { create(:user) }
  let(:device_uuid) { SecureRandom.uuid }
  let(:headers_with_device) { auth_headers(user).merge('Device-Uuid' => device_uuid) }

  describe 'GET /api/v1/users/access_devices' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/users/access_devices'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns empty list when no devices' do
        get '/api/v1/users/access_devices', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        expect(JSON.parse(response.body)).to eq([])
      end

      it 'returns list of access devices' do
        device1 = create(:access_device, user: user, first_device: true)
        device2 = create(:access_device, :aged_trusted, user: user)

        get '/api/v1/users/access_devices', headers: auth_headers(user)

        expect(response).to have_http_status(:success)
        json_response = JSON.parse(response.body)
        expect(json_response.size).to eq(2)

        # Check that both devices are present (order may vary)
        device_ids = json_response.map { |d| d['id'] }
        expect(device_ids).to contain_exactly(device1.id, device2.id)
      end
    end
  end

  describe 'GET /api/v1/users/access_devices/current' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/users/access_devices/current'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'without Device-Uuid header' do
        it 'returns error' do
          get '/api/v1/users/access_devices/current', headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('Device UUID header missing')
        end
      end

      context 'with Device-Uuid header' do
        it 'creates and returns new device' do
          expect {
            get '/api/v1/users/access_devices/current', headers: headers_with_device
          }.to change(user.access_devices, :count).by(1)

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['device_type']).to be_present
          expect(json_response['first_device']).to be true # first device
          expect(json_response['trusted']).to be false # devices start as untrusted
        end

        it 'returns existing device' do
          existing_device = create(:access_device, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))

          expect {
            get '/api/v1/users/access_devices/current', headers: headers_with_device
          }.not_to change(user.access_devices, :count)

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['id']).to eq(existing_device.id)
        end
      end
    end
  end

  describe 'DELETE /api/v1/users/access_devices/:id' do
    let(:device) { create(:access_device, user: user) }

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        delete "/api/v1/users/access_devices/#{device.id}"
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'when device exists' do
        it 'removes the device' do
          device # Force create device first
          expect {
            delete "/api/v1/users/access_devices/#{device.id}", headers: auth_headers(user)
          }.to change(user.access_devices, :count).by(-1)

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('Device removed successfully')
        end
      end

      context 'when device does not exist' do
        it 'returns not found error' do
          delete '/api/v1/users/access_devices/99999', headers: auth_headers(user)

          expect(response).to have_http_status(:not_found)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('Device not found')
        end
      end

      context 'when trying to remove the only first device' do
        it 'prevents removal' do
          first_device = create(:access_device, user: user, first_device: true)

          expect {
            delete "/api/v1/users/access_devices/#{first_device.id}", headers: auth_headers(user)
          }.not_to change(user.access_devices, :count)

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('Cannot remove the only first device')
        end
      end

      context 'when removing first device but other first devices exist' do
        it 'allows removal' do
          first_device1 = create(:access_device, user: user, first_device: true)
          first_device2 = create(:access_device, user: user, first_device: true)

          expect {
            delete "/api/v1/users/access_devices/#{first_device1.id}", headers: auth_headers(user)
          }.to change(user.access_devices, :count).by(-1)

          expect(response).to have_http_status(:success)
        end
      end
    end
  end

  describe 'POST /api/v1/users/access_devices/trust' do
    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        post '/api/v1/users/access_devices/trust'
        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      context 'without Device-Uuid header' do
        it 'returns error' do
          post '/api/v1/users/access_devices/trust', headers: auth_headers(user)

          expect(response).to have_http_status(:bad_request)
          json_response = JSON.parse(response.body)
          expect(json_response['message']).to eq('Device UUID header missing')
        end
      end

      context 'with Device-Uuid header' do
        it 'creates and trusts new device' do
          expect {
            post '/api/v1/users/access_devices/trust', headers: headers_with_device
          }.to change(user.access_devices, :count).by(1)

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['first_device']).to be true
          expect(json_response['trusted']).to be true
        end

        it 'marks existing device as trusted' do
          existing_device = create(:access_device, user: user, device_uuid_hash: AccessDevice.digest(device_uuid), first_device: false)

          expect {
            post '/api/v1/users/access_devices/trust', headers: headers_with_device
          }.not_to change(user.access_devices, :count)

          expect(response).to have_http_status(:success)
          json_response = JSON.parse(response.body)
          expect(json_response['id']).to eq(existing_device.id)
          expect(json_response['first_device']).to be true
          expect(json_response['trusted']).to be true
        end
      end
    end
  end
end
