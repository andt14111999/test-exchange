require 'rails_helper'

RSpec.describe AuthHash, type: :model do
  describe 'Info struct' do
    subject(:info) { described_class::Info.new(email: 'test@example.com', name: 'Test User', image: 'avatar.jpg') }

    it 'has email attribute' do
      expect(info.email).to eq('test@example.com')
    end

    it 'has name attribute' do
      expect(info.name).to eq('Test User')
    end

    it 'has image attribute' do
      expect(info.image).to eq('avatar.jpg')
    end
  end

  describe 'Credentials struct' do
    subject(:credentials) do
      described_class::Credentials.new(
        token: 'access_token',
        refresh_token: 'refresh_token',
        expires_at: Time.current
      )
    end

    it 'has token attribute' do
      expect(credentials.token).to eq('access_token')
    end

    it 'has refresh_token attribute' do
      expect(credentials.refresh_token).to eq('refresh_token')
    end

    it 'has expires_at attribute' do
      expect(credentials.expires_at).to be_a(Time)
    end
  end

  describe 'Extra struct' do
    subject(:extra) { described_class::Extra.new(raw_info: { id: 123 }) }

    it 'has raw_info attribute' do
      expect(extra.raw_info).to eq({ id: 123 })
    end
  end

  describe 'AuthHash class' do
    subject(:auth_hash) do
      described_class.new(
        provider: 'google',
        uid: '123456',
        info: { email: 'test@example.com', name: 'Test User', image: 'avatar.jpg' },
        credentials: { token: 'access_token', refresh_token: 'refresh_token', expires_at: Time.current },
        extra: { id: 123 }
      )
    end

    it 'has provider attribute' do
      expect(auth_hash.provider).to eq('google')
    end

    it 'has uid attribute' do
      expect(auth_hash.uid).to eq('123456')
    end

    it 'has info attribute as Info struct' do
      expect(auth_hash.info).to be_a(described_class::Info)
      expect(auth_hash.info.email).to eq('test@example.com')
      expect(auth_hash.info.name).to eq('Test User')
      expect(auth_hash.info.image).to eq('avatar.jpg')
    end

    it 'has credentials attribute as Credentials struct' do
      expect(auth_hash.credentials).to be_a(described_class::Credentials)
      expect(auth_hash.credentials.token).to eq('access_token')
      expect(auth_hash.credentials.refresh_token).to eq('refresh_token')
      expect(auth_hash.credentials.expires_at).to be_a(Time)
    end

    it 'has extra attribute as Extra struct' do
      expect(auth_hash.extra).to be_a(described_class::Extra)
      expect(auth_hash.extra.raw_info).to eq({ id: 123 })
    end
  end
end
