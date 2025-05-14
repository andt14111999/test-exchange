# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApiKey, type: :model do
  describe 'validations' do
    before do
      allow_any_instance_of(described_class).to receive(:generate_key_pair)
    end

    it 'belongs to a user' do
      api_key = described_class.new
      api_key.valid?
      expect(api_key.errors[:user]).to include("must exist")
    end

    it 'validates presence of name' do
      user = create(:user)
      api_key = described_class.new(user: user)
      api_key.valid?
      expect(api_key.errors[:name]).to include("can't be blank")
    end

    it 'validates presence of access_key' do
      user = create(:user)
      api_key = described_class.new(user: user, name: 'Test Key')
      api_key.valid?
      expect(api_key.errors[:access_key]).to include("can't be blank")
    end

    it 'validates presence of secret_key' do
      user = create(:user)
      api_key = described_class.new(user: user, name: 'Test Key')
      api_key.valid?
      expect(api_key.errors[:secret_key]).to include("can't be blank")
    end

    it 'validates uniqueness of access_key' do
      user = create(:user)
      access_key = SecureRandom.hex(16)
      secret_key = SecureRandom.hex(32)

      # Skip callbacks to ensure we're testing just the validation
      first_key = build(:api_key, user: user, name: 'First Key')
      first_key.access_key = access_key
      first_key.secret_key = secret_key
      first_key.save(validate: false)

      second_key = build(:api_key, user: user, name: 'Second Key')
      second_key.access_key = access_key
      second_key.secret_key = SecureRandom.hex(32)
      second_key.valid?

      expect(second_key.errors[:access_key]).to include("has already been taken")
    end
  end

  describe '#generate_key_pair' do
    it 'generates random keys on creation' do
      user = create(:user)
      api_key = described_class.create!(user: user, name: 'Test Key')

      expect(api_key.access_key).to be_present
      expect(api_key.secret_key).to be_present
      expect(api_key.access_key.length).to eq(32) # 16 bytes in hex
      expect(api_key.secret_key.length).to eq(64) # 32 bytes in hex
    end
  end

  describe '#encryption' do
    it 'encrypts secret key on save' do
      user = create(:user)

      # Create key pair
      access_key = SecureRandom.hex(16)
      secret_key = SecureRandom.hex(32)

      # Save the API key with the secret key
      api_key = described_class.create!(user: user, name: 'Test Key', access_key: access_key, secret_key: secret_key)

      # The stored secret key should be encrypted
      expect(api_key.secret_key).to eq(secret_key) # But attr_encrypted provides transparent access
      expect(api_key.encrypted_secret_key).to be_present # The encrypted value should exist
    end
  end

  describe '.authenticate' do
    it 'returns true for valid signature' do
      user = create(:user)
      api_key = described_class.create!(user: user, name: 'Test Key')

      # Create a message
      path = '/api/v1/resource'
      method = 'GET'
      timestamp = Time.current.to_i.to_s
      message = "#{method}#{path}#{timestamp}"

      # Generate HMAC signature
      digest = OpenSSL::Digest.new('sha256')
      signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

      expect(described_class.authenticate(api_key.access_key, signature, message)).to be true
    end

    it 'returns false for invalid signature' do
      user = create(:user)
      api_key = described_class.create!(user: user, name: 'Test Key')

      # Create a message
      path = '/api/v1/resource'
      method = 'GET'
      timestamp = Time.current.to_i.to_s
      message = "#{method}#{path}#{timestamp}"

      # Generate invalid signature
      invalid_signature = '1' * 64  # 32 bytes in hex format

      expect(described_class.authenticate(api_key.access_key, invalid_signature, message)).to be false
    end

    it 'returns false for non-existent access key' do
      message = "GET/api/v1/resource#{Time.current.to_i}"
      signature = '1' * 64
      non_existent_access_key = '0' * 32

      expect(described_class.authenticate(non_existent_access_key, signature, message)).to be false
    end

    it 'returns false for revoked API key' do
      user = create(:user)
      api_key = described_class.create!(user: user, name: 'Test Key')

      # Create a message
      path = '/api/v1/resource'
      method = 'GET'
      timestamp = Time.current.to_i.to_s
      message = "#{method}#{path}#{timestamp}"

      # Generate HMAC signature
      digest = OpenSSL::Digest.new('sha256')
      signature = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

      api_key.update!(revoked_at: Time.current)

      expect(described_class.authenticate(api_key.access_key, signature, message)).to be false
    end
  end
end
