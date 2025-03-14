# frozen_string_literal: true

class RedisFactory
  class << self
    def single_redis
      @single_redis ||= ConnectionPool::Wrapper.new(size: 20, timeout: 3) do
        Redis.new(url: ENV.fetch('REDIS_URL', 'redis://localhost:6379/0'), driver: :hiredis)
      end
    end

    def redis
      @redis ||= ConnectionPool::Wrapper.new(size: 20, timeout: 3) do
        Redis.new(url: ENV.fetch('REDIS_URL', 'redis://localhost:6379/0'), driver: :hiredis)
      end
    end
  end
end
