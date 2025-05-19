# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinConfig, type: :service do
  describe '.coins' do
    it 'returns an array of supported coins' do
      expect(described_class.coins).to be_an(Array)
      expect(described_class.coins).to include('usdt')
    end
  end

  describe '.fiats' do
    it 'returns an array of supported fiats' do
      expect(described_class.fiats).to be_an(Array)
      expect(described_class.fiats).to include('vnd', 'php', 'ngn')
    end
  end

  describe '.all' do
    it 'returns a hash of all supported currencies with their decimals' do
      expect(described_class.all).to be_a(Hash)
      expect(described_class.all).to include('usdt' => 6, 'vnd' => 0, 'php' => 2, 'ngn' => 2)
    end
  end

  describe '.get_decimal' do
    context 'when the currency is configured' do
      it 'returns the correct decimal for USDT' do
        expect(described_class.get_decimal('usdt')).to eq(6)
      end

      it 'returns the correct decimal for VND' do
        expect(described_class.get_decimal('vnd')).to eq(0)
      end

      it 'returns the correct decimal for PHP' do
        expect(described_class.get_decimal('php')).to eq(2)
      end

      it 'returns the correct decimal for NGN' do
        expect(described_class.get_decimal('ngn')).to eq(2)
      end

      it 'is case insensitive' do
        expect(described_class.get_decimal('USDT')).to eq(6)
        expect(described_class.get_decimal('VND')).to eq(0)
      end
    end

    context 'when the currency is not configured' do
      it 'returns the default decimal (8)' do
        expect(described_class.get_decimal('btc')).to eq(8)
        expect(described_class.get_decimal('eth')).to eq(8)
      end
    end
  end
end
