require 'rails_helper'

RSpec.describe Setting, type: :model do
  describe 'exchange rates' do
    describe 'default values' do
      it 'has default USDT to VND rate' do
        expect(described_class.usdt_to_vnd_rate).to eq(25000.0)
      end

      it 'has default USDT to PHP rate' do
        expect(described_class.usdt_to_php_rate).to eq(57.0)
      end

      it 'has default USDT to NGN rate' do
        expect(described_class.usdt_to_ngn_rate).to eq(450.0)
      end
    end

    describe '.get_exchange_rate' do
      it 'returns 1.0 for same currency' do
        expect(described_class.get_exchange_rate('USDT', 'USDT')).to eq(1.0)
      end

      it 'returns direct rate when available' do
        expect(described_class.get_exchange_rate('USDT', 'VND')).to eq(25000.0)
      end

      it 'returns inverse rate when direct rate not available' do
        expect(described_class.get_exchange_rate('VND', 'USDT')).to eq(1.0 / 25000.0)
      end

      it 'raises error when rate not found' do
        expect { described_class.get_exchange_rate('USD', 'EUR') }.to raise_error(StandardError)
      end
    end

    describe '.update_exchange_rate' do
      it 'updates existing rate' do
        described_class.update_exchange_rate('USDT', 'VND', 26000.0)
        expect(described_class.usdt_to_vnd_rate).to eq(26000.0)
      end

      it 'raises error when rate setting not defined' do
        expect { described_class.update_exchange_rate('USD', 'EUR', 1.2) }.to raise_error(StandardError)
      end
    end
  end

  describe '.ransackable_attributes' do
    it 'returns searchable attributes' do
      expect(described_class.ransackable_attributes).to match_array(%w[var value created_at updated_at])
    end
  end
end 