# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Setting, type: :model do
  describe 'field definitions' do
    describe 'display options' do
      it 'stores display options for exchange rate fields without min/max' do
        expect(described_class.display_options['usdt_to_vnd_rate']).to include(
          type: :number,
          step: 'any'
        )
        expect(described_class.display_options['usdt_to_vnd_rate']).not_to have_key(:min)
        expect(described_class.display_options['usdt_to_vnd_rate']).not_to have_key(:max)
      end

      it 'stores display options for withdrawal fee fields' do
        expect(described_class.display_options['usdt_erc20_withdrawal_fee']).to include(
          type: :number,
          min: 0,
          max: 100,
          step: 'any'
        )
      end

      it 'stores display options for percentage ratio fields' do
        expect(described_class.display_options['vnd_trading_fee_ratio']).to include(
          type: :number,
          min: 0.001,
          max: 1,
          step: 0.0001
        )
      end

      it 'stores display options for fixed trading fee fields' do
        expect(described_class.display_options['vnd_fixed_trading_fee']).to include(
          type: :number,
          min: 0,
          max: 999_999,
          step: 'any'
        )
      end
    end

    describe 'validation rules' do
      it 'stores validation rules for exchange rate fields' do
        expect(described_class.field_validations['usdt_to_vnd_rate']).to include(
          numericality: { greater_than: 0 }
        )
      end

      it 'stores validation rules for withdrawal fee fields' do
        expect(described_class.field_validations['usdt_erc20_withdrawal_fee']).to include(
          numericality: { greater_than_or_equal_to: 0, less_than: 100 }
        )
      end

      it 'stores validation rules for ratio fields' do
        expect(described_class.field_validations['vnd_trading_fee_ratio']).to include(
          numericality: { greater_than_or_equal_to: 0.001, less_than_or_equal_to: 1 }
        )
      end

      it 'stores validation rules for fixed trading fee fields' do
        expect(described_class.field_validations['vnd_fixed_trading_fee']).to include(
          numericality: { greater_than_or_equal_to: 0, less_than: 1_000_000 }
        )
      end
    end

    describe 'default values' do
      it 'has correct default values for exchange rates' do
        expect(described_class.usdt_to_vnd_rate).to eq(25000.0)
        expect(described_class.usdt_to_php_rate).to eq(57.0)
        expect(described_class.usdt_to_ngn_rate).to eq(450.0)
      end

      it 'has correct default values for withdrawal fees' do
        expect(described_class.usdt_erc20_withdrawal_fee).to eq(10)
        expect(described_class.usdt_bep20_withdrawal_fee).to eq(1)
        expect(described_class.usdt_solana_withdrawal_fee).to eq(3)
        expect(described_class.usdt_trc20_withdrawal_fee).to eq(2)
      end

      it 'has correct default values for trading fee ratios' do
        expect(described_class.vnd_trading_fee_ratio).to eq(0.001)
        expect(described_class.php_trading_fee_ratio).to eq(0.001)
        expect(described_class.ngn_trading_fee_ratio).to eq(0.001)
        expect(described_class.default_trading_fee_ratio).to eq(0.001)
      end

      it 'has correct default values for fixed trading fees' do
        expect(described_class.vnd_fixed_trading_fee).to eq(5000)
        expect(described_class.php_fixed_trading_fee).to eq(10)
        expect(described_class.ngn_fixed_trading_fee).to eq(300)
        expect(described_class.default_fixed_trading_fee).to eq(0)
      end
    end
  end

  describe '.get_html_input_attributes' do
    it 'returns HTML attributes for rate fields' do
      attributes = described_class.get_html_input_attributes('usdt_to_vnd_rate')
      expect(attributes).to include(
        type: 'number',
        step: 'any'
      )
      expect(attributes).not_to have_key(:min)
      expect(attributes).not_to have_key(:max)
    end

    it 'returns HTML attributes for percentage ratio fields' do
      attributes = described_class.get_html_input_attributes('vnd_trading_fee_ratio')
      expect(attributes).to include(
        type: 'number',
        min: '0.001',
        max: '1',
        step: '0.0001'
      )
    end

    it 'returns HTML attributes for fee fields' do
      attributes = described_class.get_html_input_attributes('usdt_erc20_withdrawal_fee')
      expect(attributes).to include(
        type: 'number',
        min: '0',
        max: '100',
        step: 'any'
      )
    end

    it 'returns empty hash for unknown fields' do
      attributes = described_class.get_html_input_attributes('unknown_field')
      expect(attributes).to eq({})
    end

    it 'handles fields without display options' do
      # Test for a field that might not have display options
      allow(described_class).to receive(:display_options).and_return({})
      attributes = described_class.get_html_input_attributes('any_field')
      expect(attributes).to eq({})
    end
  end

  describe '.validate_setting' do
    context 'when validating exchange rates' do
      it 'returns errors for rates at zero or below' do
        errors = described_class.validate_setting('usdt_to_vnd_rate', 0)
        expect(errors).to include('Value must be greater than 0')
      end

      it 'returns errors for negative rates' do
        errors = described_class.validate_setting('usdt_to_vnd_rate', -100)
        expect(errors).to include('Value must be greater than 0')
      end

      it 'returns no errors for valid exchange rates' do
        errors = described_class.validate_setting('usdt_to_vnd_rate', 25000)
        expect(errors).to be_empty
      end

      it 'allows very high exchange rates' do
        errors = described_class.validate_setting('usdt_to_vnd_rate', 1_000_000)
        expect(errors).to be_empty
      end

      it 'accepts decimal exchange rates' do
        errors = described_class.validate_setting('usdt_to_php_rate', 57.5)
        expect(errors).to be_empty
      end
    end

    context 'when validating withdrawal fees' do
      it 'returns errors for negative withdrawal fees' do
        errors = described_class.validate_setting('usdt_erc20_withdrawal_fee', -5)
        expect(errors).to include('Value cannot be negative')
      end

      it 'returns errors for withdrawal fees above maximum' do
        errors = described_class.validate_setting('usdt_erc20_withdrawal_fee', 101)
        expect(errors).to include('Value must be less than 100')
      end

      it 'returns no errors for valid withdrawal fees' do
        errors = described_class.validate_setting('usdt_erc20_withdrawal_fee', 10)
        expect(errors).to be_empty
      end

      it 'allows zero withdrawal fees' do
        errors = described_class.validate_setting('usdt_erc20_withdrawal_fee', 0)
        expect(errors).to be_empty
      end

      it 'allows decimal withdrawal fees' do
        errors = described_class.validate_setting('usdt_erc20_withdrawal_fee', 5.5)
        expect(errors).to be_empty
      end
    end

    context 'when validating trading fee ratios' do
      it 'returns errors for ratios below minimum' do
        errors = described_class.validate_setting('vnd_trading_fee_ratio', 0.0005)
        expect(errors).to include('Trading fee ratio must be at least 0.001 (0.1%)')
      end

      it 'returns errors for negative trading fee ratios' do
        errors = described_class.validate_setting('vnd_trading_fee_ratio', -0.01)
        expect(errors).to include('Trading fee ratio must be at least 0.001 (0.1%)')
      end

      it 'returns errors for trading fee ratios above maximum' do
        errors = described_class.validate_setting('vnd_trading_fee_ratio', 1.1)
        expect(errors).to include('Trading fee ratio must be less than or equal to 1 (100%)')
      end

      it 'returns no errors for valid trading fee ratios' do
        errors = described_class.validate_setting('vnd_trading_fee_ratio', 0.001)
        expect(errors).to be_empty
      end

      it 'allows trading fee ratios at maximum (100%)' do
        errors = described_class.validate_setting('vnd_trading_fee_ratio', 1.0)
        expect(errors).to be_empty
      end

      it 'allows mid-range trading fee ratios' do
        errors = described_class.validate_setting('vnd_trading_fee_ratio', 0.05)
        expect(errors).to be_empty
      end
    end

    context 'when validating fixed trading fees' do
      it 'returns errors for negative fixed trading fees' do
        errors = described_class.validate_setting('vnd_fixed_trading_fee', -100)
        expect(errors).to include('Value cannot be negative')
      end

      it 'returns errors for fixed trading fees above maximum' do
        errors = described_class.validate_setting('vnd_fixed_trading_fee', 1_000_000)
        expect(errors).to include('Value must be less than 1000000')
      end

      it 'returns no errors for valid fixed trading fees' do
        errors = described_class.validate_setting('vnd_fixed_trading_fee', 5000)
        expect(errors).to be_empty
      end

      it 'allows zero fixed trading fees' do
        errors = described_class.validate_setting('vnd_fixed_trading_fee', 0)
        expect(errors).to be_empty
      end

      it 'allows large fixed trading fees for VND' do
        errors = described_class.validate_setting('vnd_fixed_trading_fee', 999_999)
        expect(errors).to be_empty
      end
    end

    context 'when validating unknown fields' do
      it 'returns no errors for unknown fields' do
        errors = described_class.validate_setting('unknown_field', 'any_value')
        expect(errors).to be_empty
      end
    end

    context 'when validation rules are missing' do
      it 'returns empty array when field_validations is empty' do
        allow(described_class).to receive(:field_validations).and_return({})
        errors = described_class.validate_setting('any_field', 100)
        expect(errors).to be_empty
      end
    end
  end

  describe '.update_with_validation' do
    context 'when validation passes' do
      it 'updates the setting and returns success' do
        result = described_class.update_with_validation('usdt_to_vnd_rate', 30000)

        expect(result[:success]).to be true
        expect(result[:errors]).to be_empty
        expect(described_class.usdt_to_vnd_rate).to eq(30000)
      end

      it 'updates decimal values correctly' do
        result = described_class.update_with_validation('vnd_trading_fee_ratio', 0.005)

        expect(result[:success]).to be true
        expect(result[:errors]).to be_empty
        expect(described_class.vnd_trading_fee_ratio).to eq(0.005)
      end
    end

    context 'when validation fails' do
      it 'does not update the setting and returns errors' do
        original_value = described_class.vnd_trading_fee_ratio
        result = described_class.update_with_validation('vnd_trading_fee_ratio', 0.0005)

        expect(result[:success]).to be false
        expect(result[:errors]).to include('Trading fee ratio must be at least 0.001 (0.1%)')
        expect(described_class.vnd_trading_fee_ratio).to eq(original_value)
      end

      it 'returns multiple errors when multiple validations fail' do
        result = described_class.update_with_validation('vnd_trading_fee_ratio', -0.5)

        expect(result[:success]).to be false
        expect(result[:errors]).to include('Trading fee ratio must be at least 0.001 (0.1%)')
      end
    end

    context 'when setting does not exist' do
      it 'returns error for non-existent setting' do
        result = described_class.update_with_validation('non_existent_setting', 100)

        expect(result[:success]).to be false
        expect(result[:errors]).to include(start_with('Failed to update non_existent_setting'))
      end
    end

    context 'when update fails for other reasons' do
      it 'handles exceptions during update' do
        allow(described_class).to receive(:send).with('usdt_to_vnd_rate=', anything).and_raise(StandardError, 'Database error')

        result = described_class.update_with_validation('usdt_to_vnd_rate', 25000)

        expect(result[:success]).to be false
        expect(result[:errors]).to include('Failed to update usdt_to_vnd_rate: Database error')
      end
    end
  end

  describe '.ransackable_attributes' do
    it 'returns searchable attributes' do
      expect(described_class.ransackable_attributes).to match_array(%w[var value created_at updated_at])
    end
  end

  describe '.get_exchange_rate' do
    it 'returns 1.0 for same currency' do
      expect(described_class.get_exchange_rate('USDT', 'USDT')).to eq(1.0)
      expect(described_class.get_exchange_rate('usdt', 'usdt')).to eq(1.0)
    end

    it 'returns direct rate when available' do
      expect(described_class.get_exchange_rate('USDT', 'VND')).to eq(25000.0)
      expect(described_class.get_exchange_rate('usdt', 'vnd')).to eq(25000.0)
    end

    it 'returns inverse rate when direct rate not available' do
      rate = described_class.get_exchange_rate('VND', 'USDT')
      expect(rate).to be_within(0.000001).of(1.0 / 25000.0)
    end

    it 'raises error when rate not found' do
      expect { described_class.get_exchange_rate('USD', 'EUR') }.to raise_error(StandardError, /Exchange rate not found/)
    end

    it 'handles case insensitive currencies' do
      expect(described_class.get_exchange_rate('usdt', 'PHP')).to eq(57.0)
      expect(described_class.get_exchange_rate('USDT', 'ngn')).to eq(450.0)
    end
  end

  describe '.update_exchange_rate' do
    it 'updates existing rate' do
      described_class.update_exchange_rate('USDT', 'VND', 26000.0)
      expect(described_class.usdt_to_vnd_rate).to eq(26000.0)
    end

    it 'handles case insensitive currencies' do
      described_class.update_exchange_rate('usdt', 'php', 60.0)
      expect(described_class.usdt_to_php_rate).to eq(60.0)
    end

    it 'raises error when rate setting not defined' do
      expect { described_class.update_exchange_rate('USD', 'EUR', 1.2) }.to raise_error(StandardError, /Exchange rate setting not defined/)
    end
  end

  describe '.get_trading_fee_ratio' do
    it 'returns currency-specific trading fee ratio' do
      expect(described_class.get_trading_fee_ratio('vnd')).to eq(0.001)
      expect(described_class.get_trading_fee_ratio('VND')).to eq(0.001)
    end

    it 'returns default ratio for unknown currency' do
      expect(described_class.get_trading_fee_ratio('unknown')).to eq(0.001)
    end

    it 'handles symbol input' do
      expect(described_class.get_trading_fee_ratio(:php)).to eq(0.001)
    end

    it 'returns updated ratio after setting change' do
      described_class.php_trading_fee_ratio = 0.002
      expect(described_class.get_trading_fee_ratio('php')).to eq(0.002)
    end
  end

  describe '.get_fixed_trading_fee' do
    it 'returns currency-specific fixed trading fee' do
      expect(described_class.get_fixed_trading_fee('vnd')).to eq(5000)
      expect(described_class.get_fixed_trading_fee('VND')).to eq(5000)
    end

    it 'returns default fee for unknown currency' do
      expect(described_class.get_fixed_trading_fee('unknown')).to eq(0)
    end

    it 'handles symbol input' do
      expect(described_class.get_fixed_trading_fee(:php)).to eq(10)
    end

    it 'returns updated fee after setting change' do
      described_class.ngn_fixed_trading_fee = 500
      expect(described_class.get_fixed_trading_fee('ngn')).to eq(500)
    end
  end
end
