# frozen_string_literal: true

class CoinConfig
  DEFAULT_DECIMAL = 8

  COIN_DECIMALS = {
    'usdt' => 6
  }.freeze

  FIAT_DECIMALS = {
    'vnd' => 0,
    'php' => 2,
    'ngn' => 2
  }.freeze

  def self.coins
    COIN_DECIMALS.keys
  end

  def self.fiats
    FIAT_DECIMALS.keys
  end

  def self.all
    COIN_DECIMALS.merge(FIAT_DECIMALS)
  end

  def self.get_decimal(key)
    all[key.to_s.downcase] || DEFAULT_DECIMAL
  end
end
