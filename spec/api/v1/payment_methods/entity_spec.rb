require 'rails_helper'

RSpec.describe V1::PaymentMethods::Entity do
  describe 'exposed attributes' do
    let(:empty_entity) { described_class.represent({}) }
    let(:serialized) { empty_entity.as_json }

    it 'exposes id' do
      expect(serialized).to have_key(:id)
    end

    it 'exposes name' do
      expect(serialized).to have_key(:name)
    end

    it 'exposes display_name' do
      expect(serialized).to have_key(:display_name)
    end

    it 'exposes description' do
      expect(serialized).to have_key(:description)
    end

    it 'exposes country_code' do
      expect(serialized).to have_key(:country_code)
    end

    it 'exposes enabled' do
      expect(serialized).to have_key(:enabled)
    end

    it 'exposes icon_url' do
      expect(serialized).to have_key(:icon_url)
    end

    it 'exposes fields_required' do
      expect(serialized).to have_key(:fields_required)
    end

    it 'exposes created_at' do
      expect(serialized).to have_key(:created_at)
    end

    it 'exposes updated_at' do
      expect(serialized).to have_key(:updated_at)
    end
  end

  describe 'representation' do
    let(:payment_method_data) do
      {
        id: 1,
        name: 'bank_transfer',
        display_name: 'Bank Transfer',
        description: 'Transfer money directly from your bank account',
        country_code: 'US',
        enabled: true,
        icon_url: 'https://example.com/bank-icon.png',
        fields_required: [ 'account_number', 'routing_number' ],
        created_at: Time.zone.now,
        updated_at: Time.zone.now
      }
    end

    it 'represents payment method data correctly' do
      entity = described_class.represent(payment_method_data)
      expect(entity.as_json).to eq(payment_method_data)
    end

    it 'handles nil values' do
      entity = described_class.represent({})
      expect(entity.as_json).to eq({
        id: nil,
        name: nil,
        display_name: nil,
        description: nil,
        country_code: nil,
        enabled: nil,
        icon_url: nil,
        fields_required: nil,
        created_at: nil,
        updated_at: nil
      })
    end
  end
end
