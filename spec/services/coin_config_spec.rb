# frozen_string_literal: true

require 'rails_helper'

describe CoinConfig do
  describe '.coins' do
    it 'returns array of available coins' do
      expect(CoinConfig.coins).to eq(%w[usdt])
    end

    it 'returns array that includes usdt' do
      expect(CoinConfig.coins).to include('usdt')
    end
  end

  describe '.fiats' do
    it 'returns array of available fiats' do
      expect(CoinConfig.fiats).to eq(%w[vnd php ngn])
    end

    it 'returns array that includes vnd, php, and ngn' do
      expect(CoinConfig.fiats).to include('vnd', 'php', 'ngn')
    end
  end
end
