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
    Redis.current.get key
  end

  def key
    "Nonce:#{id}:#{nonce}"
  end

  def ttl
    Redis.current.ttl key
  end

  def save
    if valid?
      Redis.current.multi do
        Redis.current.set key, Time.now.to_i
        Redis.current.expire key, EXPIRE_TIME
      end
      @new_record = false
      true
    else
      false
    end
  end

  def del
    Redis.current.del key
  end

  private

  def ensure_uniqueness_of_nonce
    errors.add(:nonce, 'is recently used') if new_record && fetch_value
  end
end
