# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Merchant::FiatTransactionEntity, type: :entity do
  describe 'exposed attributes' do
    it 'exposes the correct attributes' do
      fiat_transaction = create(:fiat_transaction)
      entity = described_class.represent(fiat_transaction)
      serialized = entity.as_json

      expect(serialized).to include(
        id: fiat_transaction.id,
        amount: fiat_transaction.amount,
        transaction_type: fiat_transaction.transaction_type,
        currency: fiat_transaction.currency,
        created_at: fiat_transaction.created_at,
        updated_at: fiat_transaction.updated_at
      )
    end
  end
end
