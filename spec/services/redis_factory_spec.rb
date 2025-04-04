require 'rails_helper'

RSpec.describe RedisFactory, type: :service do
  describe '.single_redis' do
    it 'returns a Redis instance' do
      expect(described_class.single_redis).to be_an_instance_of(Redis)
    end

    it 'returns the same instance on multiple calls' do
      first_call = described_class.single_redis
      second_call = described_class.single_redis

      expect(first_call.object_id).to eq(second_call.object_id)
    end
  end

  describe '.redis' do
    it 'returns a ConnectionPool instance' do
      expect(described_class.redis).to be_an_instance_of(ConnectionPool)
    end

    it 'returns the same instance on multiple calls' do
      first_call = described_class.redis
      second_call = described_class.redis

      expect(first_call.object_id).to eq(second_call.object_id)
    end

    it 'creates a different instance from single_redis' do
      redis = described_class.redis
      single_redis = described_class.single_redis

      expect(redis.object_id).not_to eq(single_redis.object_id)
    end

    it 'configures pool with correct size' do
      redis = described_class.redis
      expect(redis.instance_variable_get(:@size)).to eq(20)
    end

    it 'configures pool with correct timeout' do
      redis = described_class.redis
      expect(redis.instance_variable_get(:@timeout)).to eq(5)
    end
  end
end
