# frozen_string_literal: true

class NetworkConfigurationService
  NETWORK_ORDER = {
    'erc20' => 'eth',
    'bep20' => 'bnb',
    'trc20' => 'trx',
    'bitcoin' => 'btc'
  }.freeze

  def self.base_coin_for_layer(layer)
    NETWORK_ORDER[layer]
  end

  def self.is_base_network?(coin_currency, layer)
    case coin_currency.downcase
    when 'eth'
      layer.downcase == 'erc20'
    when 'bnb'
      layer.downcase == 'bep20'
    when 'trx'
      layer.downcase == 'trc20'
    else
      false
    end
  end

  def self.get_base_layer_for_token(layer)
    case layer.downcase
    when 'erc20', 'bep20', 'trc20'
      layer.downcase
    else
      layer
    end
  end
end
