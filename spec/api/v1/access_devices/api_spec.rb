require 'rails_helper'

RSpec.describe V1::AccessDevices::Api, type: :request do
  let(:user) { create(:user) }
  let(:device_uuid) { SecureRandom.uuid }
  let(:device_headers) do
    {
      'Device-Uuid' => device_uuid,
      'Device-Type' => 'web',
      'Browser' => 'Chrome',
      'Os' => 'macOS'
    }
  end

  describe 'GET /api/v1/access_devices' do
    context 'when user is authenticated' do
      it 'returns list of access devices' do
        device = create(:access_device, user: user)

        get '/api/v1/access_devices', headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:ok)
        expect(json_response).to be_an(Array)
        expect(json_response.first).to include(
          'id' => device.id,
          'device_type' => device.device_type,
          'display_name' => device.display_name,
          'location' => device.location,
          'trusted' => device.trusted,
          'first_device' => device.first_device
        )
      end

      it 'returns devices ordered by created_at desc' do
        old_device = create(:access_device, user: user, created_at: 2.days.ago)
        new_device = create(:access_device, user: user, created_at: 1.day.ago)

        get '/api/v1/access_devices', headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:ok)
        expect(json_response.first['id']).to eq(new_device.id)
        expect(json_response.last['id']).to eq(old_device.id)
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/access_devices'

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end

  describe 'GET /api/v1/access_devices/current' do
    context 'when user is authenticated and device uuid is provided' do
      it 'returns current device info' do
        get '/api/v1/access_devices/current', headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:ok)
        expect(json_response).to include(
          'device_type' => 'web',
          'display_name' => 'Chrome (macOS)',
          'trusted' => false,
          'first_device' => true
        )
      end

      it 'creates new device if not exists' do
        expect {
          get '/api/v1/access_devices/current', headers: auth_headers(user).merge(device_headers)
        }.to change(AccessDevice, :count).by(1)

        device = AccessDevice.last
        expect(device.user).to eq(user)
        expect(device.trusted).to be false
        expect(device.first_device).to be true
      end

      it 'returns existing device if already exists' do
        device = create(:access_device, user: user, device_uuid: device_uuid)

        expect {
          get '/api/v1/access_devices/current', headers: auth_headers(user).merge(device_headers)
        }.not_to change(AccessDevice, :count)

        expect(json_response['id']).to eq(device.id)
      end
    end

    context 'when device uuid header is missing' do
      it 'returns error' do
        get '/api/v1/access_devices/current', headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response).to include('message' => 'Device UUID header missing')
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/access_devices/current'

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end

  describe 'DELETE /api/v1/access_devices/:id' do
    context 'when user is authenticated' do
      it 'removes the specified device' do
        device = create(:access_device, user: user)

        delete "/api/v1/access_devices/#{device.id}", headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:ok)
        expect(json_response).to include('message' => 'Device removed successfully')
        expect(AccessDevice.find_by(id: device.id)).to be_nil
      end

      it 'returns error when device not found' do
        delete '/api/v1/access_devices/999', headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:not_found)
        expect(json_response).to include('message' => 'Device not found')
      end

      it 'returns error when trying to remove the only first device' do
        device = create(:access_device, user: user, first_device: true)

        delete "/api/v1/access_devices/#{device.id}", headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:bad_request)
        expect(json_response).to include('message' => 'Cannot remove the only first device')
        expect(AccessDevice.find_by(id: device.id)).to be_present
      end

      it 'allows removing first device when there are other first devices' do
        device1 = create(:access_device, user: user, first_device: true)
        device2 = create(:access_device, user: user, first_device: true)

        delete "/api/v1/access_devices/#{device1.id}", headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:ok)
        expect(AccessDevice.find_by(id: device1.id)).to be_nil
        expect(AccessDevice.find_by(id: device2.id)).to be_present
      end

      it 'returns error when trying to remove another user device' do
        other_user = create(:user)
        device = create(:access_device, user: other_user)

        delete "/api/v1/access_devices/#{device.id}", headers: auth_headers(user).merge(device_headers)

        expect(response).to have_http_status(:not_found)
        expect(json_response).to include('message' => 'Device not found')
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        device = create(:access_device, user: user)
        delete "/api/v1/access_devices/#{device.id}"

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end
end
