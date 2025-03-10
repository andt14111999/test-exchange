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
  validates :currency, uniqueness: { scope: :user_id }

  scope :of_currency, ->(currency) { where(currency: currency) }

  class << self
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
end
