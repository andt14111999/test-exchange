# frozen_string_literal: true

module Admin
  module BlockchainHelper
    def blockchain_explorer_url(currency, tx_hash = nil)
      base_url = case currency.upcase
      when 'BTC'
        'https://www.blockchain.com/btc'
      when 'ETH', 'USDT-ERC20'
        'https://etherscan.io'
      when 'BNB', 'USDT-BEP20'
        'https://bscscan.com'
      when 'USDT-TRC20'
        'https://tronscan.org'
      else
        '#'
      end

      tx_hash.present? ? "#{base_url}/tx/#{tx_hash}" : base_url
    end
  end
end
