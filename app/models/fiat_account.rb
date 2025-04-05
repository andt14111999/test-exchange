# frozen_string_literal: true

class FiatAccount < ApplicationRecord
  include CategorizedAccount
  include BalanceNotification

  belongs_to :user
  has_many :fiat_transactions, dependent: :destroy

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

  class << self
    def ransackable_attributes(_auth_object = nil)
      %w[
        id user_id currency balance frozen_balance
        created_at updated_at
      ]
    end

    def ransackable_associations(_auth_object = nil)
      %w[user fiat_transactions]
    end
  end

  def available_balance
    balance - frozen_balance
  end

  def mint_amount!(amount)
    return if amount <= 0

    with_lock do
      self.balance += amount
      save!
      create_fiat_transaction(amount, 'mint')
    end
  end

  def burn_amount!(amount)
    return if amount <= 0

    with_lock do
      raise 'Insufficient balance' if amount > balance

      self.balance -= amount
      save!
      create_fiat_transaction(amount, 'burn')
    end
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
