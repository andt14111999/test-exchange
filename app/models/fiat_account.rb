# frozen_string_literal: true

class FiatAccount < ApplicationRecord
  include CategorizedAccount

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

  after_save :handle_balance_changes

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
    return unless frozen_balance > balance

    errors.add(:frozen_balance, 'cannot be greater than balance')
  end

  def handle_balance_changes
    if saved_change_to_balance? || saved_change_to_frozen_balance?
      broadcast_balance_update
      create_balance_notification
    end
  end

  def broadcast_balance_update
    BalanceBroadcastService.call(user)
  end

  def create_balance_notification
    old_balance = saved_change_to_balance? ? saved_change_to_balance[0] : balance
    new_balance = saved_change_to_balance? ? saved_change_to_balance[1] : balance

    if new_balance > old_balance
      user.notifications.create!(
        title: 'Balance Updated',
        content: "Your #{currency} balance has increased by #{new_balance - old_balance}",
        notification_type: 'balance_increase'
      )
    elsif new_balance < old_balance
      user.notifications.create!(
        title: 'Balance Updated',
        content: "Your #{currency} balance has decreased by #{old_balance - new_balance}",
        notification_type: 'balance_decrease'
      )
    end
  end

  def create_fiat_transaction(amount, transaction_type)
    fiat_transactions.create!(
      amount: amount,
      transaction_type: transaction_type,
      currency: currency
    )
  end
end
