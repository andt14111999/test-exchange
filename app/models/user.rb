# frozen_string_literal: true

class User < ApplicationRecord
  acts_as_paranoid

  # 2FA functionality
  encrypts :authenticator_key
  delegate :verify_otp, to: :otp_verifier

  has_many :social_accounts, dependent: :destroy
  has_many :coin_accounts, dependent: :destroy
  has_many :fiat_accounts, dependent: :destroy
  has_many :notifications, dependent: :destroy
  has_many :merchant_escrows, dependent: :destroy, inverse_of: :user
  has_many :merchant_escrow_operations, through: :merchant_escrows
  has_many :coin_withdrawals, dependent: :nullify
  has_many :amm_positions, dependent: :destroy
  has_many :amm_orders, dependent: :destroy
  has_many :api_keys, dependent: :destroy
  has_many :balance_locks, dependent: :destroy
  has_many :access_devices, dependent: :destroy

  # P2P Trading
  has_many :offers, dependent: :destroy
  has_many :buying_trades, class_name: 'Trade', foreign_key: 'buyer_id', dependent: :destroy
  has_many :selling_trades, class_name: 'Trade', foreign_key: 'seller_id', dependent: :destroy
  has_many :messages, dependent: :destroy
  has_many :bank_accounts, dependent: :destroy

  # Fiat operations
  has_many :fiat_deposits, dependent: :destroy
  has_many :fiat_withdrawals, dependent: :destroy

  validates :email, presence: true, uniqueness: true, format: { with: URI::MailTo::EMAIL_REGEXP }
  validates :role, inclusion: { in: %w[merchant user] }
  validates :status, inclusion: { in: %w[active suspended banned] }
  validates :kyc_level, inclusion: { in: 0..2 }
  validates :username, uniqueness: true, allow_blank: true,
                       length: { in: 3..20, allow_blank: true },
                       format: { with: /\A[a-zA-Z0-9_]+\z/, message: 'only allows letters, numbers, and underscores', allow_blank: true }
  validate :username_not_changed, if: -> { username_was.present? && username_changed? }

  # Define scopes for ActiveAdmin
  scope :merchants, -> { where(role: 'merchant') }
  scope :regular_users, -> { where(role: 'user') }
  scope :active, -> { where(status: 'active') }
  scope :suspended, -> { where(status: 'suspended') }
  scope :banned, -> { where(status: 'banned') }
  scope :phone_verified, -> { where(phone_verified: true) }
  scope :document_verified, -> { where(document_verified: true) }
  scope :snowfox_employees, -> { where(snowfox_employee: true) }

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
      username
      authenticator_enabled
      snowfox_employee
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
        token_expires_at: auth.credentials.expires_at ? Time.zone.at(auth.credentials.expires_at) : nil,
        avatar_url: auth.info.image,
        profile_data: auth.extra.raw_info
      )
      social_account.save!
    end

    social_account.user
  end

  def active?
    status == 'active'
  end

  def merchant?
    role == 'merchant'
  end

  def main_account(currency)
    currency = currency.to_s.downcase
    if CoinConfig.coins.include?(currency)
      coin_accounts.of_coin(currency).main
    else
      fiat_accounts.of_currency(currency.upcase).first
    end
  end

  def trades
    Trade.where('buyer_id = ? OR seller_id = ?', id, id)
  end

  def max_active_offers
    # For now hardcoded to 5, but could be based on user attributes
    # like kyc_level, role, etc. in the future
    5
  end

  def can_create_offer?
    # For now let merchants create offers regardless of verification
    # In production, you might want stricter requirements
    return true if merchant?

    # For regular users, require at least some verification
    # Customize these requirements based on your business logic
    phone_verified || document_verified || kyc_level > 0
  end

  # 2FA methods
  def assign_authenticator_key
    self.authenticator_key = ROTP::Base32.random_base32
  end

  def generate_provisioning_uri
    return '' if authenticator_key.blank?

    account_name = username.present? ? username : email
    return '' if account_name.blank?

    ROTP::TOTP.new(authenticator_key, issuer: 'Snowfox Exchange').provisioning_uri(account_name)
  end

  def disable_authenticator!
    self.authenticator_enabled = false
    self.authenticator_key = nil
  end

  private

  def create_default_accounts
    AccountCreationService.new(self).create_all_accounts
  end

  def username_not_changed
    errors.add(:username, 'cannot be changed once set')
  end

  def otp_verifier
    @otp_verifier ||= OtpVerifier.new(self)
  end
end
