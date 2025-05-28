# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Banks::Bank do
  describe '.from_json_array' do
    it 'converts JSON array to Bank objects' do
      json_array = [
        {
          'name' => 'Test Bank 1',
          'code' => 'TB1',
          'bin' => '123456',
          'shortName' => 'TestBank1',
          'logo' => 'https://example.com/logo1.png',
          'transferSupported' => 1,
          'lookupSupported' => 1,
          'short_name' => 'TestBank1',
          'support' => 3,
          'isTransfer' => 1,
          'swift_code' => 'TESTCODE1'
        },
        {
          'name' => 'Test Bank 2',
          'code' => 'TB2',
          'bin' => '654321',
          'shortName' => 'TestBank2',
          'logo' => 'https://example.com/logo2.png',
          'transferSupported' => 0,
          'lookupSupported' => 0,
          'short_name' => 'TestBank2',
          'support' => 0,
          'isTransfer' => 0,
          'swift_code' => 'TESTCODE2'
        }
      ]

      banks = described_class.from_json_array(json_array)

      expect(banks.length).to eq(2)
      expect(banks.first).to be_a(described_class)
      expect(banks.last).to be_a(described_class)

      # Kiểm tra thuộc tính của bank đầu tiên
      expect(banks.first.name).to eq('Test Bank 1')
      expect(banks.first.code).to eq('TB1')
      expect(banks.first.bin).to eq('123456')
      expect(banks.first.shortName).to eq('TestBank1')
      expect(banks.first.logo).to eq('https://example.com/logo1.png')
      expect(banks.first.transferSupported).to eq(1)
      expect(banks.first.lookupSupported).to eq(1)
      expect(banks.first.short_name).to eq('TestBank1')
      expect(banks.first.support).to eq(3)
      expect(banks.first.isTransfer).to eq(1)
      expect(banks.first.swift_code).to eq('TESTCODE1')

      # Kiểm tra thuộc tính của bank thứ hai
      expect(banks.last.name).to eq('Test Bank 2')
      expect(banks.last.code).to eq('TB2')
      expect(banks.last.transferSupported).to eq(0)
      expect(banks.last.isTransfer).to eq(0)
    end

    it 'handles empty array' do
      banks = described_class.from_json_array([])
      expect(banks).to be_empty
      expect(banks).to be_an(Array)
    end
  end

  describe '#initialize' do
    it 'sets attributes from hash' do
      attributes = {
        'name' => 'Test Bank',
        'code' => 'TB',
        'bin' => '123456',
        'shortName' => 'TestBank',
        'logo' => 'https://example.com/logo.png',
        'transferSupported' => 1,
        'lookupSupported' => 1,
        'short_name' => 'TestBank',
        'support' => 3,
        'isTransfer' => 1,
        'swift_code' => 'TESTCODE'
      }

      bank = described_class.new(attributes)

      expect(bank.name).to eq('Test Bank')
      expect(bank.code).to eq('TB')
      expect(bank.bin).to eq('123456')
      expect(bank.shortName).to eq('TestBank')
      expect(bank.logo).to eq('https://example.com/logo.png')
      expect(bank.transferSupported).to eq(1)
      expect(bank.lookupSupported).to eq(1)
      expect(bank.short_name).to eq('TestBank')
      expect(bank.support).to eq(3)
      expect(bank.isTransfer).to eq(1)
      expect(bank.swift_code).to eq('TESTCODE')
    end

    it 'ignores unknown attributes' do
      attributes = {
        'name' => 'Test Bank',
        'unknown_attribute' => 'value'
      }

      bank = described_class.new(attributes)

      expect(bank.name).to eq('Test Bank')
      expect(bank).not_to respond_to(:unknown_attribute)
    end
  end
end
