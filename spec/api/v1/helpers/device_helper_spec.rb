# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Helpers::DeviceHelper do
  let(:user) { create(:user) }
  let(:device_uuid) { SecureRandom.uuid }

  def build_helper(user, headers = {})
    klass = Class.new do
      include V1::Helpers::DeviceHelper
      define_method(:headers) { headers }
      define_method(:current_user) { user }
      define_method(:request) { OpenStruct.new(env: { 'action_dispatch.remote_ip' => '192.168.1.100' }) }
      define_method(:env) { {} }
    end
    klass.new
  end

  describe '#get_header' do
    it 'gets header with titleized key' do
      helper = build_helper(user, { 'Device-Uuid' => device_uuid })
      expect(helper.get_header('device-uuid')).to eq(device_uuid)
    end

    it 'gets header with X- prefix' do
      helper = build_helper(user, { 'X-Device-Uuid' => device_uuid })
      expect(helper.get_header('device-uuid')).to eq(device_uuid)
    end

    it 'returns nil when header not found' do
      helper = build_helper(user, {})
      expect(helper.get_header('device-uuid')).to be_nil
    end
  end

  describe '#device_uuid' do
    it 'returns device UUID from headers' do
      helper = build_helper(user, { 'Device-Uuid' => device_uuid })
      expect(helper.device_uuid).to eq(device_uuid)
    end

    it 'returns nil when no device UUID header' do
      helper = build_helper(user, {})
      expect(helper.device_uuid).to be_nil
    end
  end

  describe '#device_type' do
    context 'when iOS device' do
      it 'returns ios for Device-Type header' do
        helper = build_helper(user, { 'Device-Type' => 'ios' })
        expect(helper.device_type).to eq('ios')
      end
    end

    context 'when Android device' do
      it 'returns android for Device-Type header' do
        helper = build_helper(user, { 'Device-Type' => 'android' })
        expect(helper.device_type).to eq('android')
      end
    end

    context 'when web browser' do
      it 'returns web when no Device-Type header' do
        helper = build_helper(user, {})
        expect(helper.device_type).to eq('web')
      end

      it 'returns web for Device-Type header' do
        helper = build_helper(user, { 'Device-Type' => 'web' })
        expect(helper.device_type).to eq('web')
      end
    end
  end

  describe '#device_trusted_header' do
    it 'returns true when Device-Trusted header is "true"' do
      helper = build_helper(user, { 'Device-Trusted' => 'true' })
      expect(helper.device_trusted_header).to be true
    end

    it 'returns true when Device-Trusted header is "TRUE"' do
      helper = build_helper(user, { 'Device-Trusted' => 'TRUE' })
      expect(helper.device_trusted_header).to be true
    end

    it 'returns false when Device-Trusted header is "false"' do
      helper = build_helper(user, { 'Device-Trusted' => 'false' })
      expect(helper.device_trusted_header).to be false
    end

    it 'returns false when no Device-Trusted header' do
      helper = build_helper(user, {})
      expect(helper.device_trusted_header).to be false
    end
  end

  describe '#client_request_info' do
    it 'returns client info hash' do
      helper = build_helper(user, {
        'Device-Type' => 'ios',
        'Browser' => 'Chrome',
        'Os' => 'macOS'
      })

      info = helper.client_request_info
      expect(info[:device_type]).to eq('ios')
      expect(info[:browser]).to eq('Chrome')
      expect(info[:os]).to eq('macOS')
      expect(info[:ip]).to eq('192.168.1.100')
      expect(info[:country]).to eq('Unknown')
      expect(info[:city]).to eq('Unknown')
    end
  end

  describe '#current_access_device' do
    context 'when device exists' do
      it 'returns existing device' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        device = create(:access_device, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
        expect(helper.current_access_device).to eq(device)
      end
    end

    context 'when device does not exist' do
      it 'returns nil' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        expect(helper.current_access_device).to be_nil
      end
    end

    context 'when no device_uuid' do
      it 'returns nil' do
        helper = build_helper(user, {})
        expect(helper.current_access_device).to be_nil
      end
    end
  end

  describe '#create_or_find_access_device' do
    context 'when device exists' do
      it 'returns existing device' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        device = create(:access_device, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
        expect(helper.create_or_find_access_device).to eq(device)
      end
    end

    context 'when device does not exist' do
      it 'creates new device' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        expect {
          helper.create_or_find_access_device
        }.to change(AccessDevice, :count).by(1)
      end

      it 'creates device with trusted = false by default' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        device = helper.create_or_find_access_device
        expect(device.trusted).to be false
      end

      it 'marks device as trusted when Device-Trusted header is true' do
        helper = build_helper(user, {
          'Device-Uuid' => device_uuid,
          'Device-Trusted' => 'true'
        })
        device = helper.create_or_find_access_device
        expect(device.trusted).to be true
      end

      it 'marks as first device when user has no devices' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        device = helper.create_or_find_access_device
        expect(device.first_device).to be true
      end

      it 'does not mark as first device when user has devices' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        create(:access_device, user: user)
        device = helper.create_or_find_access_device
        expect(device.first_device).to be false
      end
    end

    context 'when no device_uuid' do
      it 'returns nil' do
        helper = build_helper(user, {})
        expect(helper.create_or_find_access_device).to be_nil
      end
    end
  end

  describe '#device_trusted?' do
    context 'when device exists and is trusted' do
      it 'returns true' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        create(:access_device, :trusted, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
        expect(helper.device_trusted?).to be true
      end
    end

    context 'when device exists but is not trusted' do
      it 'returns false' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        create(:access_device, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
        expect(helper.device_trusted?).to be false
      end
    end

    context 'when device does not exist' do
      it 'returns false' do
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        expect(helper.device_trusted?).to be false
      end
    end
  end

  describe '#require_2fa_for_action?' do
    context 'when user has no 2FA' do
      it 'returns false' do
        helper = build_helper(user, {})
        expect(helper.require_2fa_for_action?).to be false
      end
    end

    context 'when user has 2FA enabled' do
      it 'returns true when device is not trusted' do
        user.update!(authenticator_enabled: true)
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        expect(helper.require_2fa_for_action?).to be true
      end

      it 'returns false when device is trusted' do
        user.update!(authenticator_enabled: true)
        helper = build_helper(user, { 'Device-Uuid' => device_uuid })
        create(:access_device, :trusted, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
        expect(helper.require_2fa_for_action?).to be false
      end
    end
  end
end
