# frozen_string_literal: true

class AccountCreationService
  attr_reader :user

  SUPPORTED_NETWORKS = CoinAccount::SUPPORTED_NETWORKS.freeze
  ACCOUNT_TYPES = CoinAccount::ACCOUNT_TYPES.freeze

  def initialize(user)
    @user = user
  end

  def create_all_accounts
    create_main_accounts
    create_deposit_accounts
    create_fiat_accounts
  end

  def create_base_account(coin_currency, layer)
    return if !valid_network?(coin_currency, layer) || account_exists?(coin_currency, layer)

    create_deposit_account(coin_currency, layer)
  end

  def create_token_account(coin_currency, layer)
    return if !valid_token_network?(coin_currency, layer) || account_exists?(coin_currency, layer)

    create_deposit_account(coin_currency, layer)
  end

  def create_fiat_accounts
    FiatAccount::SUPPORTED_CURRENCIES.each_key do |currency|
      create_fiat_account(currency)
    end
  end

  private

  def create_main_accounts
    SUPPORTED_NETWORKS.keys.each { |coin_currency| create_main_account(coin_currency) }
  end

  def create_deposit_accounts
    existing_accounts = user.coin_accounts.where(account_type: 'deposit').to_a
    existing_accounts_map = existing_accounts.group_by { |acc| [ acc.coin_currency, acc.layer ] }

    accounts_to_create = []

    SUPPORTED_NETWORKS.each do |coin_currency, layers|
      layers.each do |layer|
        next if existing_accounts_map.key?([ coin_currency, layer ])

        if NetworkConfigurationService.is_base_network?(coin_currency, layer)
          accounts_to_create << { coin_currency: coin_currency, layer: layer, account_type: 'deposit' }
        else
          accounts_to_create << { coin_currency: coin_currency, layer: layer, account_type: 'deposit' }
        end
      end
    end

    return if accounts_to_create.empty?

    CoinAccount.transaction do
      accounts_to_create.each do |account_attrs|
        create_account(account_attrs[:coin_currency], account_attrs[:layer], account_attrs[:account_type])
      end
    end
  end

  def create_main_account(coin_currency)
    account = create_account(coin_currency, 'all', 'main')
    notify_kafka_service(account)
  end

  def create_deposit_account(coin_currency, layer)
    create_account(coin_currency, layer, 'deposit')
  end

  def create_fiat_account(currency)
    user.fiat_accounts.create!(
      currency: currency,
      balance: 0,
      frozen_balance: 0
    )
  end

  def create_account(coin_currency, layer, account_type)
    user.coin_accounts.create!(
      coin_currency: coin_currency,
      layer: layer,
      balance: 0,
      frozen_balance: 0,
      account_type: account_type
    )
  end

  def valid_network?(coin_currency, layer)
    SUPPORTED_NETWORKS.key?(coin_currency) &&
      SUPPORTED_NETWORKS[coin_currency].include?(layer)
  end

  def valid_token_network?(coin_currency, layer)
    valid_network?(coin_currency, layer) &&
      !NetworkConfigurationService.is_base_network?(coin_currency, layer)
  end

  def account_exists?(coin_currency, layer)
    user.coin_accounts.exists?(
      coin_currency: coin_currency,
      layer: layer,
      account_type: 'deposit'
    )
  end

  def notify_kafka_service(account)
    KafkaService::Services::Coin::CoinAccountService.new.create(
      user_id: user.id,
      coin: account.coin_currency,
      account_key: account.id
    )
  end
end
