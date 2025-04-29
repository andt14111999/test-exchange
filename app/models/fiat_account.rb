# frozen_string_literal: true

class FiatAccount < ApplicationRecord
  include CategorizedAccount
  include BalanceNotification

  belongs_to :user
  has_many :fiat_transactions, dependent: :destroy
  has_many :fiat_deposits, dependent: :destroy
  has_many :fiat_withdrawals, dependent: :destroy

  SUPPORTED_CURRENCIES = {
    'VND' => 'Vietnam Dong',
    'PHP' => 'Philippine Peso',
    'NGN' => 'Nigerian Naira'
  }.freeze

  validates :currency, presence: true, inclusion: { in: SUPPORTED_CURRENCIES.keys }
  validates :balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :frozen_balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :user_id, presence: true
  validate :validate_balances

  scope :of_currency, ->(currency) { where(currency: currency) }
  scope :of_country, ->(country_code) { where(country_code: country_code) }

  class << self
    def ransackable_attributes(_auth_object = nil)
      %w[
        id user_id currency balance frozen_balance country_code
        frozen_reason created_at updated_at
      ]
    end

    def ransackable_associations(_auth_object = nil)
      %w[user fiat_transactions fiat_deposits fiat_withdrawals]
    end
  end

  def available_balance
    balance - frozen_balance
  end

  def mint_amount!(amount)
    return if amount <= 0

    self.balance += amount
    create_fiat_transaction(amount, 'mint')
  end

  def burn_amount!(amount)
    return if amount <= 0

    raise 'Insufficient balance' if amount > balance

    self.balance -= amount
    create_fiat_transaction(amount, 'burn')
  end

  def lock_amount!(amount, reason = nil)
    return if amount <= 0

    raise 'Insufficient balance' if amount > available_balance

    self.frozen_balance += amount
    self.frozen_reason = reason if reason.present?
    save!
  end

  def unlock_amount!(amount)
    return if amount <= 0

    raise 'Insufficient frozen balance' if amount > frozen_balance

    self.frozen_balance -= amount
    self.frozen_reason = nil if frozen_balance.zero?
    save!
  end

  def account_key
    KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
      user_id: user_id,
      account_id: id
    )
  end

  private

  def validate_balances
    return if balance.nil? || frozen_balance.nil?
    return unless frozen_balance > balance

    errors.add(:frozen_balance, 'cannot be greater than balance')
  end

  def create_fiat_transaction(amount, transaction_type)
    fiat_transactions.create!(
      amount: amount,
      transaction_type: transaction_type,
      currency: currency
    )
  end
end
