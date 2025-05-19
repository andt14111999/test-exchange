# frozen_string_literal: true

require 'rails_helper'

RSpec.describe BigDecimal do
  describe '.safe_convert' do
    it 'converts a valid numeric string to BigDecimal' do
      expect(described_class.safe_convert('123.456')).to eq(BigDecimal('123.456'))
    end

    it 'converts a numeric to BigDecimal' do
      expect(described_class.safe_convert(123.456)).to eq(BigDecimal('123.456'))
    end

    it 'handles nil values' do
      expect(described_class.safe_convert(nil)).to eq(BigDecimal(0))
    end

    it 'handles empty strings' do
      expect(described_class.safe_convert('')).to eq(BigDecimal(0))
    end

    it 'handles whitespace strings' do
      expect(described_class.safe_convert('   ')).to eq(BigDecimal(0))
    end

    it 'allows custom default value' do
      expect(described_class.safe_convert(nil, 10)).to eq(BigDecimal(10))
    end

    it 'handles invalid strings gracefully' do
      expect(described_class.safe_convert('not-a-number')).to eq(BigDecimal(0))
    end
  end
end
