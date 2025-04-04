require 'rails_helper'

RSpec.describe Admin::BlockchainHelper, type: :helper do
  describe '#blockchain_explorer_url' do
    it 'returns BTC blockchain explorer URL' do
      expect(helper.blockchain_explorer_url('BTC')).to eq('https://www.blockchain.com/btc')
      expect(helper.blockchain_explorer_url('BTC', 'tx123')).to eq('https://www.blockchain.com/btc/tx/tx123')
    end

    it 'returns ETH blockchain explorer URL' do
      expect(helper.blockchain_explorer_url('ETH')).to eq('https://etherscan.io')
      expect(helper.blockchain_explorer_url('ETH', 'tx123')).to eq('https://etherscan.io/tx/tx123')
    end

    it 'returns USDT-ERC20 blockchain explorer URL' do
      expect(helper.blockchain_explorer_url('USDT-ERC20')).to eq('https://etherscan.io')
      expect(helper.blockchain_explorer_url('USDT-ERC20', 'tx123')).to eq('https://etherscan.io/tx/tx123')
    end

    it 'returns BNB blockchain explorer URL' do
      expect(helper.blockchain_explorer_url('BNB')).to eq('https://bscscan.com')
      expect(helper.blockchain_explorer_url('BNB', 'tx123')).to eq('https://bscscan.com/tx/tx123')
    end

    it 'returns USDT-BEP20 blockchain explorer URL' do
      expect(helper.blockchain_explorer_url('USDT-BEP20')).to eq('https://bscscan.com')
      expect(helper.blockchain_explorer_url('USDT-BEP20', 'tx123')).to eq('https://bscscan.com/tx/tx123')
    end

    it 'returns USDT-TRC20 blockchain explorer URL' do
      expect(helper.blockchain_explorer_url('USDT-TRC20')).to eq('https://tronscan.org')
      expect(helper.blockchain_explorer_url('USDT-TRC20', 'tx123')).to eq('https://tronscan.org/tx/tx123')
    end

    it 'returns default URL for unknown currency' do
      expect(helper.blockchain_explorer_url('UNKNOWN')).to eq('#')
      expect(helper.blockchain_explorer_url('UNKNOWN', 'tx123')).to eq('#/tx/tx123')
    end

    it 'handles case-insensitive currency' do
      expect(helper.blockchain_explorer_url('btc')).to eq('https://www.blockchain.com/btc')
      expect(helper.blockchain_explorer_url('eth')).to eq('https://etherscan.io')
      expect(helper.blockchain_explorer_url('bnb')).to eq('https://bscscan.com')
    end
  end
end
