# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PaymentMethod, type: :model do
  describe 'associations' do
    it { is_expected.to have_many(:offers).dependent(:nullify) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:name) }

    it 'validates uniqueness of name' do
      payment_method = create(:payment_method, name: 'unique_name')
      duplicate = build(:payment_method, name: 'unique_name')
      expect(duplicate).to be_invalid
    end

    it { is_expected.to validate_presence_of(:display_name) }
    it { is_expected.to validate_presence_of(:country_code) }
    it { is_expected.to validate_presence_of(:fields_required) }
  end

  describe 'scopes' do
    describe '.enabled' do
      it 'returns only enabled payment methods' do
        enabled_payment_method = create(:payment_method, enabled: true)
        disabled_payment_method = create(:payment_method, enabled: false)

        expect(described_class.enabled).to include(enabled_payment_method)
        expect(described_class.enabled).not_to include(disabled_payment_method)
      end
    end

    describe '.disabled' do
      it 'returns only disabled payment methods' do
        enabled_payment_method = create(:payment_method, enabled: true)
        disabled_payment_method = create(:payment_method, enabled: false)

        expect(described_class.disabled).to include(disabled_payment_method)
        expect(described_class.disabled).not_to include(enabled_payment_method)
      end
    end

    describe '.of_country' do
      it 'returns payment methods matching the country code' do
        us_payment_method = create(:payment_method, country_code: 'US')
        vn_payment_method = create(:payment_method, country_code: 'VN')

        expect(described_class.of_country('US')).to include(us_payment_method)
        expect(described_class.of_country('US')).not_to include(vn_payment_method)
      end
    end
  end

  describe '.ransackable_attributes' do
    it 'returns the allowed attributes for ransack' do
      expected_attributes = %w[
        id name display_name description
        country_code enabled icon_url
        fields_required created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns the allowed associations for ransack' do
      expect(described_class.ransackable_associations).to contain_exactly('offers')
    end
  end

  describe '#enable!' do
    it 'enables a payment method' do
      payment_method = create(:payment_method, enabled: false)

      payment_method.enable!

      expect(payment_method.enabled).to be true
      expect(payment_method.reload.enabled).to be true
    end
  end

  describe '#disable!' do
    it 'disables a payment method' do
      payment_method = create(:payment_method, enabled: true)

      payment_method.disable!

      expect(payment_method.enabled).to be false
      expect(payment_method.reload.enabled).to be false
    end
  end

  describe '#required_fields' do
    it 'returns symbolized keys from fields_required' do
      fields = { 'account_number' => 'text', 'bank_name' => 'select' }
      payment_method = create(:payment_method, fields_required: fields)

      expected_result = { account_number: 'text', bank_name: 'select' }
      expect(payment_method.required_fields).to eq(expected_result)
    end
  end
end
