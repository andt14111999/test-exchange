# frozen_string_literal: true

module BalanceNotification
  extend ActiveSupport::Concern

  included do
    after_commit :handle_balance_changes, on: :update
  end

  private

  def handle_balance_changes
    if saved_change_to_balance? || saved_change_to_frozen_balance?
      broadcast_balance_update
    end

    if saved_change_to_balance?
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
        content: "Your #{currency_identifier} balance has increased by #{new_balance - old_balance}",
        notification_type: 'balance_increase'
      )
    elsif new_balance < old_balance
      user.notifications.create!(
        title: 'Balance Updated',
        content: "Your #{currency_identifier} balance has decreased by #{old_balance - new_balance}",
        notification_type: 'balance_decrease'
      )
    end
  end

  def currency_identifier
    respond_to?(:coin_currency) ? coin_currency.upcase : currency
  end
end
