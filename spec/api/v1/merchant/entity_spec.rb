require 'rails_helper'

RSpec.describe V1::Merchant::Entity do
  describe 'exposed attributes' do
    let(:empty_entity) { described_class.represent({}) }
    let(:serialized) { empty_entity.as_json }

    it 'exposes id' do
      expect(serialized).to have_key(:id)
    end

    it 'exposes usdt_amount' do
      expect(serialized).to have_key(:usdt_amount)
    end

    it 'exposes fiat_amount' do
      expect(serialized).to have_key(:fiat_amount)
    end

    it 'exposes fiat_currency' do
      expect(serialized).to have_key(:fiat_currency)
    end

    it 'exposes status' do
      expect(serialized).to have_key(:status)
    end
  end

  describe 'representation' do
    let(:merchant_data) do
      {
        id: 1,
        usdt_amount: '1000.00',
        fiat_amount: '25000.00',
        fiat_currency: 'VND',
        status: 'active'
      }
    end

    it 'represents merchant data correctly' do
      entity = described_class.represent(merchant_data)
      expect(entity.as_json).to eq(merchant_data)
    end

    it 'handles nil values' do
      entity = described_class.represent({})
      expect(entity.as_json).to eq({
        id: nil,
        usdt_amount: nil,
        fiat_amount: nil,
        fiat_currency: nil,
        status: nil
      })
    end
  end
end
