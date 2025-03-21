# frozen_string_literal: true

class User < ApplicationRecord
  has_many :social_accounts, dependent: :destroy
  has_many :coin_accounts, dependent: :destroy
  has_many :fiat_accounts, dependent: :destroy

  validates :email, presence: true, uniqueness: true, format: { with: URI::MailTo::EMAIL_REGEXP }
  validates :role, inclusion: { in: %w[merchant user] }
  validates :status, inclusion: { in: %w[active suspended banned] }
  validates :kyc_level, inclusion: { in: 0..2 }

  # Define scopes for ActiveAdmin
  scope :merchants, -> { where(role: 'merchant') }
  scope :regular_users, -> { where(role: 'user') }
  scope :active, -> { where(status: 'active') }
  scope :suspended, -> { where(status: 'suspended') }
  scope :banned, -> { where(status: 'banned') }
  scope :phone_verified, -> { where(phone_verified: true) }
  scope :document_verified, -> { where(document_verified: true) }

  # Required for Ransack search in ActiveAdmin
  def self.ransackable_attributes(_auth_object = nil)
    %w[
      avatar_url
      created_at
      display_name
      document_verified
      email
      id
      kyc_level
      phone_verified
      role
      status
      updated_at
    ]
  end

  # Required for Ransack associations in ActiveAdmin
  def self.ransackable_associations(_auth_object = nil)
    %w[social_accounts]
  end

  after_create :create_default_accounts

  def self.from_social_auth(auth)
    social_account = SocialAccount.find_or_initialize_by(
      provider: auth.provider,
      provider_user_id: auth.uid
    )

    if social_account.new_record?
      user = User.find_or_create_by!(email: auth.info.email) do |u|
        u.display_name = auth.info.name
        u.avatar_url = auth.info.image
        u.role = 'user'
        u.status = 'active'
      end

      social_account.assign_attributes(
        user: user,
        email: auth.info.email,
        name: auth.info.name,
        access_token: auth.credentials.token,
        refresh_token: auth.credentials.refresh_token,
        token_expires_at: auth.credentials.expires_at&.to_datetime,
        avatar_url: auth.info.image,
        profile_data: auth.extra.raw_info
      )
      social_account.save!
    end

    social_account.user
  end

  private

  def create_default_accounts
    CoinAccount::SUPPORTED_NETWORKS.each do |coin_currency, layers|
      create_main_coin_account(coin_currency)
      create_deposit_coin_accounts(coin_currency, layers)
    end

    create_fiat_accounts
  end

  def create_main_coin_account(coin_currency)
    main_account = coin_accounts.create!(
      coin_currency: coin_currency,
      layer: 'all',
      balance: 0,
      frozen_balance: 0,
      account_type: 'main'
    )

    send_event_create_coin_account_to_kafka(main_account)
  end

  def create_deposit_coin_accounts(coin_currency, layers)
    network_addresses = {}

    layers.each do |layer|
      coin_account = create_deposit_account(coin_currency, layer)

      if base_network_coin?(coin_currency, layer)
        address = generate_coin_address(coin_account)
        network_addresses[layer] = address if address
        coin_account.update!(address: address) if address
      else
        base_address = network_addresses[layer]
        coin_account.update!(address: base_address) if base_address
      end
    end
  end

  def create_deposit_account(coin_currency, layer)
    coin_accounts.create!(
      coin_currency: coin_currency,
      layer: layer,
      balance: 0,
      frozen_balance: 0,
      account_type: 'deposit'
    )
  end

  def generate_coin_address(coin_account)
    result, ok = get_coin_address(coin_account: coin_account)

    if ok
      result['address']
    else
      Rails.logger.error("Failed to generate coin address for account #{coin_account.id}") if Rails.env.production?
      nil
    end
  end

  def base_network_coin?(coin_currency, layer)
    case layer.downcase
    when 'erc20'
      coin_currency.downcase == 'eth'
    when 'bep20'
      coin_currency.downcase == 'bnb'
    else
      false
    end
  end

  def create_fiat_accounts
    FiatAccount::SUPPORTED_CURRENCIES.each_key do |currency|
      fiat_accounts.create!(
        currency: currency,
        balance: 0,
        frozen_balance: 0
      )
    end
  end

  def send_event_create_coin_account_to_kafka(coin_account)
    client.create(
      user_id: id, coin: coin_account.coin_currency, account_key: coin_account.id
    )
  end

  def get_coin_address(coin_account:)
    PostbackService.new(
      target_url: 'https://coin-portal.exchange.snowfoxglobal.org/api/v1/coin_addresses',
      payload: {
        account_type: coin_account.account_type,
        coin: coin_account.coin_currency,
        account_id: coin_account.id
      }
    ).post
  end

  def client
    @client ||= KafkaService::Services::Coin::CoinAccountService.new
  end
end
