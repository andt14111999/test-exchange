# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Merchant::EscrowEntity, type: :entity do
  describe 'exposed attributes' do
    it 'exposes the correct attributes' do
      escrow = create(:merchant_escrow)
      entity = described_class.represent(escrow)
      serialized = entity.as_json

      expect(serialized).to include(
        id: escrow.id,
        usdt_amount: escrow.usdt_amount,
        fiat_amount: escrow.fiat_amount,
        fiat_currency: escrow.fiat_currency,
        status: escrow.status,
        created_at: escrow.created_at,
        updated_at: escrow.updated_at
      )
    end
  end
end
