# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Users::Entity, type: :entity do
  describe 'exposed attributes' do
    it 'exposes the correct attributes' do
      user = create(:user)
      entity = described_class.represent(user)
      serialized = entity.as_json

      expect(serialized).to include(
        id: user.id,
        email: user.email,
        username: user.username,
        display_name: user.display_name,
        avatar_url: user.avatar_url,
        role: user.role,
        status: user.status,
        kyc_level: user.kyc_level,
        phone_verified: user.phone_verified,
        document_verified: user.document_verified
      )
    end
  end
end
