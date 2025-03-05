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
    # Create coin accounts for each coin type and its supported layers
    CoinAccount::SUPPORTED_NETWORKS.each do |coin_type, layers|
      layers.each do |layer|
        coin_accounts.create!(
          coin_type: coin_type,
          layer: layer,
          balance: 0,
          frozen_balance: 0,
          total_balance: 0,
          available_balance: 0
        )
      end
    end

    # Create fiat accounts for each supported currency
    FiatAccount::SUPPORTED_CURRENCIES.each_key do |currency|
      fiat_accounts.create!(
        currency: currency,
        balance: 0,
        frozen_balance: 0,
        total_balance: 0,
        available_balance: 0
      )
    end
  end
end
