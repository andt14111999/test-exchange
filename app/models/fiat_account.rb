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

  after_save :handle_balance_changes

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

  private

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
        content: "Your #{currency.upcase} balance has increased by #{new_balance - old_balance}",
        notification_type: 'balance_increase'
      )
    elsif new_balance < old_balance
      user.notifications.create!(
        title: 'Balance Updated',
        content: "Your #{currency.upcase} balance has decreased by #{old_balance - new_balance}",
        notification_type: 'balance_decrease'
      )
    end
  end
end
