# frozen_string_literal: true

class OtpVerifier
  attr_reader :admin_user

  delegate :id, :authenticator_key, to: :admin_user

  def initialize(admin_user)
    @admin_user = admin_user
  end

  def verify_otp(code)
    return false if authenticator_key.blank?
    return false unless valid_code?(code)

    create_nonce(code)
  end

  private

  def valid_code?(code)
    # ROTP::TOTP#verify returns nil when fail and a timestamp when pass
    totp.verify(code.to_s, drift_ahead: 60, drift_behind: 60).present?
  end

  def create_nonce(code)
    Nonce.new(id, code).save
  end

  def totp
    @totp ||= ROTP::TOTP.new(authenticator_key)
  end
end
