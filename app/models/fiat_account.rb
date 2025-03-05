# frozen_string_literal: true

class FiatAccount < ApplicationRecord
  belongs_to :user

  SUPPORTED_CURRENCIES = {
    'VNDS' => 'Vietnam Dong Stable',
    'PHPS' => 'Philippine Peso Stable'
  }.freeze

  validates :currency, presence: true, inclusion: { in: SUPPORTED_CURRENCIES.keys }
  validates :balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :frozen_balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :total_balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :available_balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :currency, uniqueness: { scope: :user_id }
  validate :validate_balances

  scope :of_currency, ->(currency) { where(currency: currency) }

  scope :total_balances, lambda {
    select(
      'COALESCE(SUM(balance), 0) as balance',
      'COALESCE(SUM(frozen_balance), 0) as frozen_balance',
      'COALESCE(SUM(total_balance), 0) as total_balance',
      'COALESCE(SUM(available_balance), 0) as available_balance'
    ).reorder(nil)
  }

  before_validation :calculate_balances

  BalanceInfo = Struct.new(:balance, :frozen_balance, :total_balance, :available_balance)

  class << self
    def main
      result = total_balances.take

      BalanceInfo.new(
        balance: result&.balance || 0,
        frozen_balance: result&.frozen_balance || 0,
        total_balance: result&.total_balance || 0,
        available_balance: result&.available_balance || 0
      )
    end

    def ransackable_attributes(_auth_object = nil)
      %w[
        id user_id currency
        balance frozen_balance total_balance available_balance
        created_at updated_at
      ]
    end

    def ransackable_associations(_auth_object = nil)
      %w[user]
    end
  end

  private

  def calculate_balances
    self.total_balance = balance + frozen_balance
    self.available_balance = balance - frozen_balance
  end

  def validate_balances
    errors.add(:frozen_balance, 'cannot be greater than balance') if frozen_balance > balance

    errors.add(:available_balance, 'cannot be negative') if available_balance.negative?

    return unless total_balance != (balance + frozen_balance)

      errors.add(:total_balance, 'must equal balance + frozen_balance')
  end
end
