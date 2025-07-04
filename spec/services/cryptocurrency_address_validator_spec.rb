# frozen_string_literal: true

require 'rails_helper'

describe CryptocurrencyAddressValidator do
  describe '#valid?' do
    context 'when validating Solana addresses' do
      it 'returns true for valid Solana address' do
        valid_solana_address = 'CaG9bbefgTHxExuDEmpTzG89Xz7uYWKmrCabUzG2QKh9'
        validator = described_class.new(valid_solana_address, 'solana')

        expect(validator.valid?).to be true
      end

      it 'returns false for invalid Solana address' do
        invalid_solana_address = '0x1234567890abcdef' # ETH format
        validator = described_class.new(invalid_solana_address, 'solana')

        expect(validator.valid?).to be false
      end

      it 'returns false for too short Solana address' do
        short_address = '123'
        validator = described_class.new(short_address, 'solana')

        expect(validator.valid?).to be false
      end

      it 'returns false for invalid base58 characters' do
        invalid_chars = '0OIl1111111111111111111111111111' # contains invalid chars
        validator = described_class.new(invalid_chars, 'solana')

        expect(validator.valid?).to be false
      end
    end

    context 'when validating Bitcoin layer addresses' do
      it 'returns true for valid Bitcoin address using gem' do
        valid_btc_address = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        validator = described_class.new(valid_btc_address, 'bitcoin')

        allow(AdequateCryptoAddress).to receive(:valid?).with(valid_btc_address, :bitcoin).and_return(true)

        expect(validator.valid?).to be true
      end

      it 'returns false for invalid Bitcoin address' do
        invalid_btc_address = 'invalid_address'
        validator = described_class.new(invalid_btc_address, 'bitcoin')

        allow(AdequateCryptoAddress).to receive(:valid?).with(invalid_btc_address, :bitcoin).and_return(false)

        expect(validator.valid?).to be false
      end
    end

    context 'when validating EVM addresses (ERC20/BEP20)' do
      it 'returns true for valid ERC20 address using gem' do
        valid_eth_address = '0xde709f2102306220921060314715629080e2fb77'
        validator = described_class.new(valid_eth_address, 'erc20')

        allow(AdequateCryptoAddress).to receive(:valid?).with(valid_eth_address, :ethereum).and_return(true)

        expect(validator.valid?).to be true
      end

      it 'returns true for valid BEP20 address using gem' do
        valid_bep20_address = '0xde709f2102306220921060314715629080e2fb77'
        validator = described_class.new(valid_bep20_address, 'bep20')

        allow(AdequateCryptoAddress).to receive(:valid?).with(valid_bep20_address, :ethereum).and_return(true)

        expect(validator.valid?).to be true
      end

      it 'returns false for invalid EVM address' do
        invalid_eth_address = 'invalid_eth_address'
        validator = described_class.new(invalid_eth_address, 'erc20')

        allow(AdequateCryptoAddress).to receive(:valid?).with(invalid_eth_address, :ethereum).and_return(false)

        expect(validator.valid?).to be false
      end
    end

    context 'when validating TRON addresses (TRC20)' do
      it 'returns true for valid TRON address' do
        valid_tron_address = 'TLyqzVGLV1srkB7dToTAEqgDSfPtXRJZYH'
        validator = described_class.new(valid_tron_address, 'trc20')

        expect(validator.valid?).to be true
      end

      it 'returns false for invalid TRON address - wrong length' do
        invalid_tron_address = 'TLyqzVGLV1srkB7dToTAEqgDSfPtXRJZY' # 33 chars instead of 34
        validator = described_class.new(invalid_tron_address, 'trc20')

        expect(validator.valid?).to be false
      end

      it 'returns false for invalid TRON address - wrong prefix' do
        invalid_tron_address = 'BLyqzVGLV1srkB7dToTAEqgDSfPtXRJZYH' # starts with B instead of T
        validator = described_class.new(invalid_tron_address, 'trc20')

        expect(validator.valid?).to be false
      end

      it 'returns false for invalid TRON address - invalid base58' do
        invalid_tron_address = 'TLyqzVGLV1srkB7dToTAEqgDSfPtXRJZ0I' # contains 0 and I
        validator = described_class.new(invalid_tron_address, 'trc20')

        expect(validator.valid?).to be false
      end
    end

    context 'when validating unsupported layers' do
      it 'raises ArgumentError for unsupported layer' do
        address = 'some_address'
        validator = described_class.new(address, 'unsupported_layer')

        expect { validator.valid? }.to raise_error(ArgumentError, /Layer 'unsupported_layer' is not supported/)
      end

      it 'logs warning and returns true for supported but unimplemented layer' do
        # Mock SUPPORTED_LAYERS to include a layer not in the case statement
        address = 'some_address'
        validator = described_class.new(address, 'new_unsupported_layer')

        # Add the layer to supported layers temporarily
        stub_const('CryptocurrencyAddressValidator::SUPPORTED_LAYERS', [ 'erc20', 'bep20', 'trc20', 'bitcoin', 'solana', 'new_unsupported_layer' ])
        allow(Rails.logger).to receive(:warn)

        result = validator.valid?

        expect(result).to be true
        expect(Rails.logger).to have_received(:warn).with("Address validation not implemented for layer: new_unsupported_layer")
      end
    end

    context 'when handling errors' do
      it 'returns false and logs error when gem raises exception' do
        address = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        validator = described_class.new(address, 'bitcoin')

        allow(AdequateCryptoAddress).to receive(:valid?).and_raise(StandardError.new('Test error'))
        allow(Rails.logger).to receive(:error)

        expect(validator.valid?).to be false
        expect(Rails.logger).to have_received(:error).with("Gem validation error for bitcoin address #{address}: Test error")
      end

      it 'returns false and logs error when gem raises exception for ERC20' do
        address = '0xde709f2102306220921060314715629080e2fb77'
        validator = described_class.new(address, 'erc20')

        allow(AdequateCryptoAddress).to receive(:valid?).and_raise(StandardError.new('Gem error'))
        allow(Rails.logger).to receive(:error)

        expect(validator.valid?).to be false
        expect(Rails.logger).to have_received(:error).with("Gem validation error for ethereum address #{address}: Gem error")
      end

      it 'returns false and logs error when any other method raises exception' do
        address = 'TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE'
        validator = described_class.new(address, 'trc20')

        # Mock valid_tron_address? to raise exception
        allow(validator).to receive(:valid_tron_address?).and_raise(StandardError.new('TRON validation error'))
        allow(Rails.logger).to receive(:error)

        expect(validator.valid?).to be false
        expect(Rails.logger).to have_received(:error).with("Error validating trc20 address #{address}: TRON validation error")
      end
    end

    context 'when address or layer is blank' do
      it 'returns false for blank address' do
        validator = described_class.new('', 'bitcoin')

        expect(validator.valid?).to be false
      end

      it 'returns false for blank layer' do
        validator = described_class.new('some_address', '')

        expect(validator.valid?).to be false
      end

      it 'returns false for nil address' do
        validator = described_class.new(nil, 'bitcoin')

        expect(validator.valid?).to be false
      end

      it 'returns false for nil layer' do
        validator = described_class.new('some_address', nil)

        expect(validator.valid?).to be false
      end
    end
  end
end
