class ApiKey < ApplicationRecord
  include Ransackable

  attr_encrypted :secret_key,
    key: proc { Rails.application.credentials.seed_encryption_key || ENV['SEED_ENCRYPTION_KEY'] },
    encode: true,
    encode_iv: true,
    encode_salt: true

  belongs_to :user

  validates :name, presence: true
  validates :access_key, presence: true, uniqueness: true
  validates :secret_key, presence: true

  before_validation :generate_key_pair, on: :create, unless: -> { access_key.present? && encrypted_secret_key.present? }

  def self.authenticate(access_key, signature, message)
    api_key = find_by(access_key: access_key)
    return false unless api_key
    return false if api_key.revoked_at.present?

    begin
      # Use OpenSSL to create HMAC SHA256 signature
      digest = OpenSSL::Digest.new('sha256')
      expected_hmac = OpenSSL::HMAC.hexdigest(digest, api_key.secret_key, message)

      # Compare signatures using secure comparison
      ActiveSupport::SecurityUtils.secure_compare(expected_hmac, signature)
    rescue StandardError
      false
    end
  end

  private

  def generate_key_pair
    self.access_key = SecureRandom.hex(16)
    self.secret_key = SecureRandom.hex(32)
  end
end
