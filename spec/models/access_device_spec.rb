# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AccessDevice, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:user) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:device_uuid_hash) }
    it { is_expected.to validate_presence_of(:details) }

    context 'uniqueness validation' do
      it 'validates uniqueness of device_uuid_hash scoped to user_id' do
        user = create(:user)
        create(:access_device, user: user, device_uuid_hash: 'test-hash')

        duplicate_device = build(:access_device, user: user, device_uuid_hash: 'test-hash')
        expect(duplicate_device).to be_invalid
        expect(duplicate_device.errors[:device_uuid_hash]).to include('has already been taken')
      end
    end
  end

  describe 'store_accessor' do
    it 'provides accessor methods for details fields' do
      device = create(:access_device)
      expect(device).to respond_to(:device_type)
      expect(device).to respond_to(:browser)
      expect(device).to respond_to(:os)
      expect(device).to respond_to(:ip)
      expect(device).to respond_to(:city)
      expect(device).to respond_to(:country)
    end
  end

  describe '#device_uuid=' do
    it 'sets device_uuid_hash as MD5 digest of device_uuid' do
      device = described_class.new
      uuid = SecureRandom.uuid
      device.device_uuid = uuid

      expect(device.device_uuid_hash).to eq(Digest::MD5.hexdigest(uuid))
    end
  end

  describe '#display_name' do
    it 'returns formatted display name' do
      device = create(:access_device)
      expected = "#{device.browser} (#{device.os})"
      expect(device.display_name).to eq(expected)
    end
  end

  describe '#location' do
    it 'returns formatted location' do
      device = create(:access_device)
      expected = "#{device.city}, #{device.country}"
      expect(device.location).to eq(expected)
    end
  end

  describe '.digest' do
    it 'returns MD5 hexdigest of the key' do
      key = 'test-key'
      expected = Digest::MD5.hexdigest(key)
      expect(described_class.digest(key)).to eq(expected)
    end

    it 'returns nil for blank key' do
      expect(described_class.digest(nil)).to be_nil
      expect(described_class.digest('')).to be_nil
    end
  end

  describe '.find_by_device_uuid' do
    it 'finds device by UUID' do
      uuid = SecureRandom.uuid
      device = create(:access_device)
      device.device_uuid = uuid
      device.save!

      found_device = described_class.find_by_device_uuid(uuid)
      expect(found_device).to eq(device)
    end
  end

  describe '#trusted' do
    it 'defaults to false' do
      device = create(:access_device)
      expect(device.trusted).to be false
    end

    it 'can be set to true' do
      device = create(:access_device, :trusted)
      expect(device.trusted).to be true
    end
  end

  describe '#mark_as_trusted!' do
    it 'updates trusted field to true' do
      device = create(:access_device)
      expect(device.trusted).to be false

      device.mark_as_trusted!
      expect(device.reload.trusted).to be true
    end
  end
end
