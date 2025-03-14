# frozen_string_literal: true

class RedisFactory
  def self.redis
    @redis ||= ConnectionPool.new(size: 20, timeout: 5) { Redis.new(url:) }
  end

  def self.redis_obj
    @redis_obj ||= Redis.new(url:)
  end

  def self.single_redis
    @single_redis ||= ConnectionPool::Wrapper.new(size: 10, timeout: 5) { Redis.new(url:) }
  end

  def self.reset
    redis.with(&:flushall)
  end

  def self.shutdown
    redis.shutdown(&:close)
    @redis = nil
  end

  def self.nest(key)
    Nest.new(key, redis_obj)
  end

  def self.url
    return ENV.fetch('REDIS_URL', nil) if Rails.env.production?
    return ENV.fetch('REDIS_URL', 'redis://127.0.0.1:6379') if Rails.env.development?

    ENV['REDIS_TEST_URL'] || 'redis://127.0.0.1:6379'
  end
end
