# frozen_string_literal: true

Redis.class_eval do
  def scan_in_batch(match:, count: 1000, cursor: 0)
    loop do
      cursor, keys = scan(cursor, match: match, count: count)
      yield(keys, cursor)

      break if cursor == '0'
    end
  end
end

Rails.application.config.to_prepare do
  Redis.current = RedisFactory.redis
  RedisClassy.redis = RedisFactory.single_redis
end
