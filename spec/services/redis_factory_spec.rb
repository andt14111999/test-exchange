require 'rails_helper'
require 'nest'

RSpec.describe RedisFactory do
  before do
    described_class.instance_variable_set(:@single_redis, nil)
    described_class.instance_variable_set(:@redis, nil)
    described_class.instance_variable_set(:@redis_obj, nil)
    allow(ENV).to receive(:[]).and_call_original
  end

  describe '.single_redis' do
    it 'creates a new Redis connection pool' do
      redis = described_class.single_redis
      expect(redis).to be_a(Redis)
    end

    it 'returns the same instance on subsequent calls' do
      first_call = described_class.single_redis
      second_call = described_class.single_redis
      expect(first_call.object_id).to eq(second_call.object_id)
    end

    it 'creates Redis connection with correct configuration' do
      redis_double = instance_double(Redis)
      wrapper_double = instance_double(ConnectionPool::Wrapper)

      allow(Redis).to receive(:new).with(
        url: 'redis://127.0.0.1:6379'
      ).and_return(redis_double)
      allow(ConnectionPool::Wrapper).to receive(:new).with(
        size: 10,
        timeout: 5
      ).and_yield.and_return(wrapper_double)

      described_class.single_redis

      expect(Redis).to have_received(:new)
      expect(ConnectionPool::Wrapper).to have_received(:new)
    end
  end

  describe '.redis' do
    it 'creates a new Redis connection pool' do
      redis = described_class.redis
      expect(redis).to be_a(ConnectionPool)
    end

    it 'returns the same instance on subsequent calls' do
      first_call = described_class.redis
      second_call = described_class.redis
      expect(first_call.object_id).to eq(second_call.object_id)
    end

    it 'creates Redis connection with correct configuration' do
      redis_double = instance_double(Redis)
      pool_double = instance_double(ConnectionPool)

      allow(Redis).to receive(:new).with(
        url: 'redis://127.0.0.1:6379'
      ).and_return(redis_double)
      allow(ConnectionPool).to receive(:new).with(
        size: 20,
        timeout: 5
      ).and_yield.and_return(pool_double)

      described_class.redis

      expect(Redis).to have_received(:new)
      expect(ConnectionPool).to have_received(:new)
    end
  end

  describe '.redis_obj' do
    it 'creates a new Redis instance' do
      redis = described_class.redis_obj
      expect(redis).to be_a(Redis)
    end

    it 'returns the same instance on subsequent calls' do
      first_call = described_class.redis_obj
      second_call = described_class.redis_obj
      expect(first_call.object_id).to eq(second_call.object_id)
    end
  end

  describe '.reset' do
    it 'flushes all data from Redis' do
      redis_double = instance_double(Redis)
      pool_double = instance_double(ConnectionPool)

      allow(described_class).to receive(:redis).and_return(pool_double)
      allow(pool_double).to receive(:with).and_yield(redis_double)
      allow(redis_double).to receive(:flushall)

      described_class.reset

      expect(redis_double).to have_received(:flushall)
    end
  end

  describe '.shutdown' do
    it 'closes Redis connection and resets instance variable' do
      redis_double = instance_double(Redis)
      pool_double = instance_double(ConnectionPool)

      allow(described_class).to receive(:redis).and_return(pool_double)
      allow(pool_double).to receive(:shutdown).and_yield(redis_double)
      allow(redis_double).to receive(:close)

      described_class.shutdown

      expect(redis_double).to have_received(:close)
      expect(described_class.instance_variable_get(:@redis)).to be_nil
    end
  end

  describe '.nest' do
    it 'creates a new Nest instance with the given key' do
      redis_obj = instance_double(Redis)
      nest_instance = instance_double(Nest)

      allow(described_class).to receive(:redis_obj).and_return(redis_obj)
      allow(Nest).to receive(:new).with('test_key', redis_obj).and_return(nest_instance)

      result = described_class.nest('test_key')

      expect(result).to eq(nest_instance)
      expect(Nest).to have_received(:new).with('test_key', redis_obj)
    end
  end

  describe '.url' do
    context 'in production environment' do
      before do
        allow(Rails.env).to receive_messages(production?: true, development?: false)
      end

      it 'returns REDIS_URL from ENV or nil' do
        allow(ENV).to receive(:fetch).with('REDIS_URL', nil).and_return('redis://prod:6379')
        expect(described_class.url).to eq('redis://prod:6379')
      end
    end

    context 'in development environment' do
      before do
        allow(Rails.env).to receive_messages(production?: false, development?: true)
      end

      it 'returns REDIS_URL from ENV or default development URL' do
        allow(ENV).to receive(:fetch).with('REDIS_URL', 'redis://127.0.0.1:6379').and_return('redis://dev:6379')
        expect(described_class.url).to eq('redis://dev:6379')
      end
    end

    context 'in test environment' do
      before do
        allow(Rails.env).to receive_messages(production?: false, development?: false, test?: true)
      end

      it 'returns REDIS_TEST_URL from ENV or default test URL' do
        allow(ENV).to receive(:[]).with('REDIS_TEST_URL').and_return('redis://test:6379')
        expect(described_class.url).to eq('redis://test:6379')
      end

      it 'returns default test URL when REDIS_TEST_URL is not set' do
        allow(ENV).to receive(:[]).with('REDIS_TEST_URL').and_return(nil)
        expect(described_class.url).to eq('redis://127.0.0.1:6379')
      end
    end
  end
end
