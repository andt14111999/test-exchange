# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OtpVerifier, type: :model do
  describe '#verify_otp' do
    it 'returns false when authenticator_key is blank' do
      admin_user = create(:admin_user)
      verifier = described_class.new(admin_user)
      expect(verifier.verify_otp('123456')).to be false
    end

    it 'returns false when code is invalid' do
      admin_user = create(:admin_user)
      admin_user.assign_authenticator_key
      verifier = described_class.new(admin_user)
      expect(verifier.verify_otp('invalid')).to be false
    end

    it 'creates nonce and returns true when code is valid' do
      admin_user = create(:admin_user)
      admin_user.assign_authenticator_key
      verifier = described_class.new(admin_user)

      valid_code = ROTP::TOTP.new(admin_user.authenticator_key).now
      expect(verifier.verify_otp(valid_code)).to be true
    end
  end
end
