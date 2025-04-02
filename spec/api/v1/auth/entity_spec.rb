require 'rails_helper'

RSpec.describe V1::Auth::Entity do
  describe 'exposed attributes' do
    let(:empty_entity) { described_class.represent({}) }
    let(:serialized) { empty_entity.as_json }

    it 'exposes token' do
      expect(serialized).to have_key(:token)
    end
  end

  describe 'representation' do
    let(:auth_data) do
      {
        token: 'jwt-token-123'
      }
    end

    it 'represents auth data correctly' do
      entity = described_class.represent(auth_data)
      expect(entity.as_json).to eq(auth_data)
    end

    it 'handles nil values' do
      entity = described_class.represent({})
      expect(entity.as_json).to eq({
        token: nil
      })
    end
  end
end
