# frozen_string_literal: true

class CryptocurrencyAddressValidator
  # Get all supported layers from CoinAccount
  SUPPORTED_LAYERS = CoinAccount::SUPPORTED_NETWORKS.values.flatten.uniq.freeze

  def initialize(address, layer)
    @address = address
    @layer = layer&.downcase
  end

  def valid?
    return false if @address.blank? || @layer.blank?

    # Normalize layer name for Bitcoin (accept both 'btc' and 'bitcoin')
    normalized_layer = normalize_layer(@layer)

    # Check if layer is supported (including both btc and bitcoin)
    unless SUPPORTED_LAYERS.include?(normalized_layer) || (normalized_layer == 'btc' && SUPPORTED_LAYERS.include?('bitcoin'))
      raise ArgumentError, "Layer '#{@layer}' is not supported. Supported layers: #{SUPPORTED_LAYERS.join(', ')}"
    end

    case normalized_layer
    when 'solana'
      valid_solana_address?
    when 'erc20', 'bep20'
      # ERC20 (Ethereum) and BEP20 (BSC) both use EVM address format
      valid_crypto_address_with_gem?(:ethereum)
    when 'trc20'
      # TRON network - uses different address format
      valid_tron_address?
    when 'bitcoin', 'btc'
      valid_crypto_address_with_gem?(:bitcoin)
    else
      # For any other layers, log warning and return true to not block
      Rails.logger.warn("Address validation not implemented for layer: #{normalized_layer}")
      true
    end
  rescue ArgumentError
    # Re-raise ArgumentError for unsupported layers
    raise
  rescue StandardError => e
    Rails.logger.error("Error validating #{@layer} address #{@address}: #{e.message}")
    false
  end

  private

  def normalize_layer(layer)
    # Accept both 'btc' and 'bitcoin' for Bitcoin network
    layer == 'btc' ? 'btc' : layer
  end

  def valid_crypto_address_with_gem?(currency_symbol)
    AdequateCryptoAddress.valid?(@address, currency_symbol)
  rescue StandardError => e
    Rails.logger.error("Gem validation error for #{currency_symbol} address #{@address}: #{e.message}")
    false
  end

  def valid_tron_address?
    return false if @address.blank?

    # TRON addresses start with 'T' and are 34 characters long (base58 encoded)
    return false unless @address.length == 34
    return false unless @address.start_with?('T')

    # Check if it's valid base58
    valid_base58?(@address)
  end

  def valid_solana_address?
    return false if @address.blank?

    # Basic length check (Solana addresses are typically 44 characters when base58 encoded)
    return false unless @address.length.between?(32, 44)

    # Check if it's valid base58
    valid_base58?(@address)
  end

  def valid_base58?(string)
    # Base58 alphabet (Bitcoin/Solana standard)
    base58_alphabet = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
    string.chars.all? { |char| base58_alphabet.include?(char) }
  end
end
