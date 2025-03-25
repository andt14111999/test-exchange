# frozen_string_literal: true

class AccountCreationService
  attr_reader :user, :network_addresses

  def initialize(user)
    @user = user
    @network_addresses = {}
  end

  def create_all_accounts
    create_main_accounts
    create_network_accounts
    create_fiat_accounts
  end

  private

  def create_main_accounts
    CoinAccount::SUPPORTED_NETWORKS.keys.each do |coin_currency|
      create_main_account(coin_currency)
    end
  end

  def create_network_accounts
    NetworkConfigurationService::NETWORK_ORDER.each do |network, base_coin|
      create_base_network_accounts(network, base_coin)
      create_token_accounts(network, base_coin)
    end
  end

  def create_base_network_accounts(network, base_coin)
    return unless should_create_base_account?(base_coin, network)

    create_deposit_accounts(base_coin, [ network ])
  end

  def create_token_accounts(network, base_coin)
    CoinAccount::SUPPORTED_NETWORKS.each do |coin_currency, layers|
      next if coin_currency == base_coin
      next unless layers.include?(network)
      create_deposit_accounts(coin_currency, [ network ])
    end
  end

  def should_create_base_account?(base_coin, network)
    CoinAccount::SUPPORTED_NETWORKS.key?(base_coin) &&
      CoinAccount::SUPPORTED_NETWORKS[base_coin].include?(network)
  end

  def create_main_account(coin_currency)
    account = user.coin_accounts.create!(
      coin_currency: coin_currency,
      layer: 'all',
      balance: 0,
      frozen_balance: 0,
      account_type: 'main'
    )

    notify_kafka_service(account)
  end

  def create_deposit_accounts(coin_currency, layers)
    layers.each do |layer|
      next if account_exists?(coin_currency, layer)

      if NetworkConfigurationService.is_base_network?(coin_currency, layer)
        create_base_network_account(coin_currency, layer)
      else
        create_token_account(coin_currency, layer)
      end
    end
  end

  def create_base_network_account(coin_currency, layer)
    account = create_deposit_account(coin_currency, layer)
    address = generate_address(account)

    if address
      store_network_address(layer, address, coin_currency)
      update_account_address(account, address)
    end
  end

  def create_token_account(coin_currency, layer)
    account = create_deposit_account(coin_currency, layer)
    base_layer = NetworkConfigurationService.get_base_layer_for_token(layer)
    base_address = network_addresses[base_layer]

    if base_address&.dig(:address)
      update_account_address(account, base_address[:address])
    else
      log_missing_address(coin_currency, layer)
    end
  end

  def create_deposit_account(coin_currency, layer)
    user.coin_accounts.create!(
      coin_currency: coin_currency,
      layer: layer,
      balance: 0,
      frozen_balance: 0,
      account_type: 'deposit'
    )
  end

  def account_exists?(coin_currency, layer)
    user.coin_accounts.where(
      coin_currency: coin_currency,
      layer: layer,
      account_type: 'deposit'
    ).exists?
  end

  def generate_address(account)
    AddressGenerationService.new(account).generate
  end

  def store_network_address(layer, address, coin_currency)
    network_addresses[layer] = {
      address: address,
      base_coin: coin_currency
    }
  end

  def update_account_address(account, address)
    account.update!(address: address)
  end

  def create_fiat_accounts
    FiatAccount::SUPPORTED_CURRENCIES.each_key do |currency|
      user.fiat_accounts.create!(
        currency: currency,
        balance: 0,
        frozen_balance: 0
      )
    end
  end

  def notify_kafka_service(account)
    KafkaService::Services::Coin::CoinAccountService.new.create(
      user_id: user.id,
      coin: account.coin_currency,
      account_key: account.id
    )
  end

  def log_missing_address(coin_currency, layer)
    Rails.logger.error("No base address found for #{coin_currency}/#{layer}")
  end
end
