require 'rails_helper'

RSpec.describe V1::Auth::Entity do
  describe 'exposed attributes' do
    let(:empty_entity) { described_class.represent({}) }
    let(:serialized) { empty_entity.as_json }

    it 'exposes token' do
      expect(serialized).to have_key(:token)
    end

    it 'exposes user' do
      expect(serialized).to have_key(:user)
    end
  end

  describe 'representation' do
    let(:user) { create(:user, email: 'test@example.com', display_name: 'Test User') }
    let(:auth_data) do
      {
        token: 'jwt-token-123',
        user: user
      }
    end

    it 'represents auth data correctly' do
      entity = described_class.represent(auth_data)
      serialized = entity.as_json

      expect(serialized[:token]).to eq('jwt-token-123')
      expect(serialized[:user]).to include(
        id: user.id,
        email: user.email,
        display_name: user.display_name,
        role: user.role,
        status: user.status,
        kyc_level: user.kyc_level,
        phone_verified: user.phone_verified,
        document_verified: user.document_verified
      )
    end

    it 'handles nil values' do
      entity = described_class.represent({})
      expect(entity.as_json).to eq({
        token: nil,
        user: nil
      })
    end
  end
end
