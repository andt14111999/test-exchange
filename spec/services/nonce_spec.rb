require 'rails_helper'

RSpec.describe Nonce, type: :service do
  describe 'validations' do
    it 'validates nonce format' do
      nonce = described_class.new('test_id', 'valid123')
      expect(nonce).to be_valid

      nonce = described_class.new('test_id', 'invalid!@#')
      expect(nonce).to be_invalid
      expect(nonce.errors[:nonce]).to include('is invalid')
    end

    it 'validates uniqueness of nonce' do
      nonce1 = described_class.new('test_id', 'valid123')
      nonce1.save

      nonce2 = described_class.new('test_id', 'valid123')
      expect(nonce2).to be_invalid
      expect(nonce2.errors[:base]).to include('already_used')
    end
  end

  describe 'initialization' do
    it 'sets id and nonce' do
      nonce = described_class.new('test_id', 'valid123')
      expect(nonce.id).to eq('test_id')
      expect(nonce.nonce).to eq('valid123')
      expect(nonce.new_record).to be true
    end
  end

  describe '#key' do
    it 'returns correct key format' do
      nonce = described_class.new('test_id', 'valid123')
      expect(nonce.key).to eq('Nonce:test_id:valid123')
    end
  end

  describe '#fetch_value' do
    it 'returns value from Redis' do
      nonce = described_class.new('test_id', 'valid123')
      nonce.save

      value = nonce.fetch_value
      expect(value).to be_present
      expect(value.to_i).to be_within(1).of(Time.now.to_i)
    end
  end

  describe '#ttl' do
    it 'returns time to live from Redis' do
      nonce = described_class.new('test_id', 'valid123')
      nonce.save

      ttl = nonce.ttl
      expect(ttl).to be_present
      expect(ttl).to be <= Nonce::EXPIRE_TIME
    end
  end

  describe '#save' do
    it 'saves nonce to Redis and sets expiration' do
      nonce = described_class.new('test_id', 'valid123')
      expect(nonce.save).to be true
      expect(nonce.new_record).to be false

      ttl = nonce.ttl
      expect(ttl).to be_present
      expect(ttl).to be <= Nonce::EXPIRE_TIME
    end
  end

  describe '#del' do
    it 'deletes nonce from Redis' do
      nonce = described_class.new('test_id', 'valid123')
      nonce.save
      expect(nonce.fetch_value).to be_present

      nonce.del
      expect(nonce.fetch_value).to be_nil
    end
  end
end
