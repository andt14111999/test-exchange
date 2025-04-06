require 'rails_helper'

RSpec.describe NetworkConfigurationService, type: :service do
  describe '.base_coin_for_layer' do
    it 'returns eth for erc20' do
      expect(described_class.base_coin_for_layer('erc20')).to eq('eth')
    end

    it 'returns bnb for bep20' do
      expect(described_class.base_coin_for_layer('bep20')).to eq('bnb')
    end

    it 'returns trx for trc20' do
      expect(described_class.base_coin_for_layer('trc20')).to eq('trx')
    end

    it 'returns btc for bitcoin' do
      expect(described_class.base_coin_for_layer('bitcoin')).to eq('btc')
    end

    it 'returns nil for unknown layer' do
      expect(described_class.base_coin_for_layer('unknown')).to be_nil
    end
  end

  describe '.is_base_network?' do
    context 'when coin is ETH' do
      it 'returns true for erc20 layer' do
        expect(described_class.is_base_network?('ETH', 'erc20')).to be true
      end

      it 'returns false for other layers' do
        expect(described_class.is_base_network?('ETH', 'bep20')).to be false
      end
    end

    context 'when coin is BNB' do
      it 'returns true for bep20 layer' do
        expect(described_class.is_base_network?('BNB', 'bep20')).to be true
      end

      it 'returns false for other layers' do
        expect(described_class.is_base_network?('BNB', 'erc20')).to be false
      end
    end

    context 'when coin is TRX' do
      it 'returns true for trc20 layer' do
        expect(described_class.is_base_network?('TRX', 'trc20')).to be true
      end

      it 'returns false for other layers' do
        expect(described_class.is_base_network?('TRX', 'erc20')).to be false
      end
    end

    context 'when coin is not a base network coin' do
      it 'returns false' do
        expect(described_class.is_base_network?('USDT', 'erc20')).to be false
      end
    end
  end

  describe '.get_base_layer_for_token' do
    it 'returns erc20 for erc20 layer' do
      expect(described_class.get_base_layer_for_token('ERC20')).to eq('erc20')
    end

    it 'returns bep20 for bep20 layer' do
      expect(described_class.get_base_layer_for_token('BEP20')).to eq('bep20')
    end

    it 'returns trc20 for trc20 layer' do
      expect(described_class.get_base_layer_for_token('TRC20')).to eq('trc20')
    end

    it 'returns original layer for unknown layer' do
      expect(described_class.get_base_layer_for_token('UNKNOWN')).to eq('UNKNOWN')
    end
  end
end
