# frozen_string_literal: true

require 'rails_helper'

RSpec.describe JsonWebToken do
  describe '.encode' do
    it 'encodes payload with expiration' do
      payload = { user_id: 1 }
      exp = 1.hour.from_now
      token = described_class.encode(payload, exp)

      expect(token).to be_a(String)
      decoded_payload = JWT.decode(token, described_class::SECRET_KEY, true, { algorithm: 'HS256' }).first

      expect(decoded_payload['user_id']).to eq(1)
      expect(decoded_payload['exp']).to eq(exp.to_i)
    end

    it 'uses default expiration of 24 hours when not provided' do
      payload = { user_id: 1 }
      token = described_class.encode(payload)

      decoded_payload = JWT.decode(token, described_class::SECRET_KEY, true, { algorithm: 'HS256' }).first
      expected_exp = 24.hours.from_now.to_i

      # Allow 1 second difference due to processing time
      expect(decoded_payload['exp']).to be_within(1).of(expected_exp)
    end
  end

  describe '.decode' do
    it 'decodes a valid token' do
      payload = { user_id: 1 }
      token = described_class.encode(payload)

      decoded = described_class.decode(token)

      expect(decoded).to be_a(HashWithIndifferentAccess)
      expect(decoded[:user_id]).to eq(1)
    end

    it 'returns nil for an invalid token' do
      expect(described_class.decode('invalid_token')).to be_nil
    end

    it 'returns nil for an expired token' do
      payload = { user_id: 1 }
      token = described_class.encode(payload, 1.second.ago)

      expect(described_class.decode(token)).to be_nil
    end

    it 'returns nil for a token with invalid signature' do
      payload = { user_id: 1 }
      token = JWT.encode(payload, 'wrong_secret', 'HS256')

      expect(described_class.decode(token)).to be_nil
    end
  end
end
