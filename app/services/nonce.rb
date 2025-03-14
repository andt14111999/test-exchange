# frozen_string_literal: true

class Nonce
  include ActiveModel::Validations
  EXPIRE_TIME = 20.minutes

  validates :nonce, format: /\A[a-zA-Z0-9]{6,32}\z/
  validate :ensure_uniqueness_of_nonce

  attr_reader :id, :nonce, :new_record

  def initialize(id, nonce)
    @id = id
    @nonce = nonce
    @new_record = true
  end

  def fetch_value
    RedisFactory.single_redis.with do |redis|
      redis.get(key)
    end
  end

  def key
    "Nonce:#{id}:#{nonce}"
  end

  def ttl
    RedisFactory.single_redis.with do |redis|
      redis.ttl(key)
    end
  end

  def save
    RedisFactory.single_redis.with do |redis|
      redis.set(key, Time.now.to_i)
      redis.expire(key, EXPIRE_TIME) if ttl.present?
    end
    @new_record = false
    true
  end

  def del
    RedisFactory.single_redis.with do |redis|
      redis.del(key)
    end
  end

  private

  def ensure_uniqueness_of_nonce
    RedisFactory.single_redis.with do |redis|
      return true unless redis.exists?(key)

      errors.add(:base, :already_used)
      false
    end
  end
end
