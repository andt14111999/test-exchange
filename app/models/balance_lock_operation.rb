# frozen_string_literal: true

class BalanceLockOperation < ApplicationRecord
  include AASM

  acts_as_paranoid

  delegate :locked_balances, to: :balance_lock

  belongs_to :balance_lock
  has_many :coin_transactions, as: :operation, dependent: :destroy
  has_many :fiat_transactions, as: :operation, dependent: :destroy

  validates :operation_type, presence: true, inclusion: { in: %w[lock release] }
  validates :status, presence: true

  after_create :auto_process!

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :processing
    state :completed
    state :failed

    event :process do
      transitions from: [ :pending ], to: :processing
    end

    event :complete do
      transitions from: [ :processing ], to: :completed
    end

    event :fail do
      transitions from: [ :pending, :processing ], to: :failed
    end
  end

  def user
    balance_lock.user
  end

  def auto_process!
    ActiveRecord::Base.transaction do
      process!

      case operation_type
      when 'lock'
        lock_user_balances
      when 'release'
        unlock_user_balances
      end

      complete!
    end
    true
  rescue StandardError => e
    Rails.logger.error("Failed to process balance lock operation: #{e.message}")
    update(status: 'failed', status_explanation: e.message)
    false
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id balance_lock_id operation_type
      status status_explanation created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[balance_lock coin_transactions fiat_transactions]
  end

  private

  def lock_user_balances
    locked_balances.each do |currency, amount|
      # Try to find coin account first
      coin_account = user.coin_accounts.of_coin(currency).first
      if coin_account
        coin_transactions.create!(
          amount: -amount.to_d,
          coin_currency: currency,
          coin_account: coin_account,
          transaction_type: 'lock',
          snapshot_balance: coin_account.balance,
          snapshot_frozen_balance: coin_account.frozen_balance
        )
        next
      end

      # Try to find fiat account
      fiat_account = user.fiat_accounts.of_currency(currency).first
      if fiat_account
        fiat_transactions.create!(
          amount: -amount.to_d,
          currency: currency,
          fiat_account: fiat_account,
          transaction_type: 'lock',
          snapshot_balance: fiat_account.balance,
          snapshot_frozen_balance: fiat_account.frozen_balance
        )
      end
    end
  end

  def unlock_user_balances
    locked_balances.each do |currency, amount|
      # Try to find coin account first
      coin_account = user.coin_accounts.of_coin(currency).first
      if coin_account
        coin_transactions.create!(
          amount: amount.to_d,
          coin_currency: currency,
          coin_account: coin_account,
          transaction_type: 'unlock',
          snapshot_balance: coin_account.balance,
          snapshot_frozen_balance: coin_account.frozen_balance
        )
        next
      end

      # Try to find fiat account
      fiat_account = user.fiat_accounts.of_currency(currency).first
      if fiat_account
        fiat_transactions.create!(
          amount: amount.to_d,
          currency: currency,
          fiat_account: fiat_account,
          transaction_type: 'unlock',
          snapshot_balance: fiat_account.balance,
          snapshot_frozen_balance: fiat_account.frozen_balance
        )
      end
    end
  end
end
