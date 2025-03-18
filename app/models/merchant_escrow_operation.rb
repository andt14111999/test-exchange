# frozen_string_literal: true

class MerchantEscrowOperation < ApplicationRecord
  include AASM

  belongs_to :merchant_escrow
  belongs_to :usdt_account, class_name: 'CoinAccount'
  belongs_to :fiat_account
  has_many :coin_transactions, as: :operation, dependent: :destroy
  has_many :fiat_transactions, as: :operation, dependent: :destroy

  validates :usdt_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_currency, presence: true
  validates :operation_type, presence: true, inclusion: { in: %w[freeze unfreeze] }

  delegate :user, to: :merchant_escrow

  after_create :process_operation

  scope :sorted, -> { order(created_at: :desc) }

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :processing
    state :completed
    state :failed

    event :process do
      transitions from: [ :pending ], to: :processing
    end

    event :complete do
      transitions from: [ :processing ], to: :completed,
        after: :create_transactions
    end

    event :fail do
      transitions from: [ :processing ], to: :failed
    end
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[created_at fiat_account_id fiat_amount fiat_currency id merchant_escrow_id
       operation_type status status_explanation updated_at usdt_account_id usdt_amount]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_transactions fiat_account fiat_transactions merchant_escrow usdt_account]
  end

  private

  def process_operation
    process!
    complete!
  rescue StandardError => e
    fail!
    Rails.logger.error("Failed to process merchant escrow operation: #{e.message}")
    raise
  end

  def create_transactions
    ActiveRecord::Base.transaction do
      if operation_type == 'freeze'
        create_freeze_transactions
      else
        create_unfreeze_transactions
      end
    end
  end

  def create_freeze_transactions
    # Freeze USDT
    coin_transactions.create!(
      coin_account: usdt_account,
      coin_currency: 'usdt',
      amount: -usdt_amount
    )

    # Increase fiat balance
    fiat_transactions.create!(
      fiat_account: fiat_account,
      currency: fiat_currency,
      amount: fiat_amount,
      transaction_type: 'escrow_deposit'
    )

    # Update account balances
    usdt_account.with_lock do
      usdt_account.update!(
        frozen_balance: usdt_account.frozen_balance + usdt_amount
      )
    end

    fiat_account.with_lock do
      fiat_account.update!(
        balance: fiat_account.balance + fiat_amount
      )
    end
  end

  def create_unfreeze_transactions
    # Unfreeze USDT
    coin_transactions.create!(
      coin_account: usdt_account,
      coin_currency: 'usdt',
      amount: usdt_amount
    )

    # Decrease fiat balance
    fiat_transactions.create!(
      fiat_account: fiat_account,
      currency: fiat_currency,
      amount: -fiat_amount,
      transaction_type: 'escrow_withdrawal'
    )

    # Update account balances
    usdt_account.with_lock do
      usdt_account.update!(
        frozen_balance: usdt_account.frozen_balance - usdt_amount
      )
    end

    fiat_account.with_lock do
      fiat_account.update!(
        balance: fiat_account.balance - fiat_amount
      )
    end
  end
end
