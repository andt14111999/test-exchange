require 'rails_helper'

describe 'to_bool extension', type: :initializer do
  describe 'String#to_bool' do
    it 'converts truthy strings to true' do
      expect('true'.to_bool).to be true
      expect('t'.to_bool).to be true
      expect('yes'.to_bool).to be true
      expect('y'.to_bool).to be true
      expect('1'.to_bool).to be true

      # Case insensitive
      expect('TRUE'.to_bool).to be true
      expect('Yes'.to_bool).to be true
    end

    it 'converts falsey strings to false' do
      expect('false'.to_bool).to be false
      expect('f'.to_bool).to be false
      expect('no'.to_bool).to be false
      expect('n'.to_bool).to be false
      expect('0'.to_bool).to be false

      # Case insensitive
      expect('FALSE'.to_bool).to be false
      expect('No'.to_bool).to be false
    end

    it 'converts empty string to false' do
      expect(''.to_bool).to be false
    end

    it 'raises ArgumentError for non-boolean strings' do
      expect { 'hello'.to_bool }.to raise_error(ArgumentError)
      expect { '2'.to_bool }.to raise_error(ArgumentError)
    end
  end

  describe 'Integer#to_bool' do
    it 'converts 1 to true' do
      expect(1.to_bool).to be true
    end

    it 'converts 0 to false' do
      expect(0.to_bool).to be false
    end

    it 'raises ArgumentError for other integers' do
      expect { 2.to_bool }.to raise_error(ArgumentError)
      expect { -1.to_bool }.to raise_error(ArgumentError)
    end
  end

  describe 'TrueClass#to_bool' do
    it 'returns self' do
      expect(true.to_bool).to be true
    end

    it 'converts to integer 1' do
      expect(true.to_i).to eq(1)
    end
  end

  describe 'FalseClass#to_bool' do
    it 'returns self' do
      expect(false.to_bool).to be false
    end

    it 'converts to integer 0' do
      expect(false.to_i).to eq(0)
    end
  end

  describe 'NilClass#to_bool' do
    it 'returns false' do
      expect(nil.to_bool).to be false
    end
  end
end
