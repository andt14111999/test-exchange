require 'rails_helper'

RSpec.describe V1::FiatAccount::Entity do
  describe 'exposed attributes' do
    let(:empty_entity) { described_class.represent({}) }
    let(:serialized) { empty_entity.as_json }

    it 'exposes currency' do
      expect(serialized).to have_key(:currency)
    end

    it 'exposes currency_name' do
      expect(serialized).to have_key(:currency_name)
    end

    it 'exposes balance' do
      expect(serialized).to have_key(:balance)
    end

    it 'exposes frozen_balance' do
      expect(serialized).to have_key(:frozen_balance)
    end
  end

  describe 'representation' do
    let(:fiat_account_data) do
      {
        currency: 'USD',
        currency_name: 'US Dollar',
        balance: '1000.00',
        frozen_balance: '100.00'
      }
    end

    it 'represents fiat account data correctly' do
      entity = described_class.represent(fiat_account_data)
      expect(entity.as_json).to eq(fiat_account_data)
    end

    it 'handles nil values' do
      entity = described_class.represent({})
      expect(entity.as_json).to eq({
        currency: nil,
        currency_name: nil,
        balance: nil,
        frozen_balance: nil
      })
    end
  end
end
