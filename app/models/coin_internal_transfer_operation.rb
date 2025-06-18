# frozen_string_literal: true

class CoinInternalTransferOperation < ApplicationRecord
  include AASM

  validates :coin_currency, :coin_amount, :status, presence: true
  validates :coin_amount, numericality: { greater_than: 0 }

  belongs_to :coin_withdrawal, touch: true
  belongs_to :sender, class_name: 'User'
  belongs_to :receiver, class_name: 'User'
  has_many :coin_transactions, as: :operation, dependent: :destroy

  after_initialize :set_default_values
  before_validation :set_coin_currency, on: :create
  after_create :create_coin_transactions
  after_create :auto_process!

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :processing
    state :completed
    state :rejected
    state :canceled

    event :process do
      transitions from: [ :pending ], to: :processing
    end

    event :complete do
      transitions from: [ :processing ], to: :completed, after: :complete_withdrawal
    end

    event :reject do
      transitions from: [ :pending, :processing ], to: :rejected
    end

    event :cancel do
      transitions from: [ :pending ], to: :canceled
    end
  end

  def required_coin_amount
    coin_amount + coin_fee.to_d
  end

  # Alias method to maintain compatibility
  def reason
    status_explanation
  end

  # Alias method to maintain compatibility
  def reason=(value)
    self.status_explanation = value
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id coin_withdrawal_id sender_id receiver_id coin_currency coin_amount
      coin_fee status status_explanation created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_withdrawal sender receiver coin_transactions]
  end

  # Process the internal transfer automatically
  def auto_process!
    begin
      ActiveRecord::Base.transaction do
        process!
        complete!
      end
      true
    rescue StandardError => e
      Rails.logger.error("Failed to process internal transfer: #{e.message}")
      update(status: 'rejected', status_explanation: e.message)
      false
    end
  end

  private

  def complete_withdrawal
    # Mark withdrawal as processing and then completed
    withdrawal = coin_withdrawal
    withdrawal.process! if withdrawal.may_process?
    withdrawal.complete! if withdrawal.may_complete?
  end

  def create_coin_transactions
    ActiveRecord::Base.transaction do
      coin_transactions.create!(
        amount: -required_coin_amount,
        coin_currency: coin_currency,
        coin_account: sender.coin_accounts.of_coin(coin_currency).main
      )
      coin_transactions.create!(
        amount: required_coin_amount,
        coin_currency: coin_currency,
        coin_account: receiver.coin_accounts.of_coin(coin_currency).main
      )
    end
  end

  def set_default_values
    if new_record?
      self.coin_amount = coin_withdrawal.try(:coin_amount)
      self.coin_fee = coin_withdrawal.try(:coin_fee).to_d
      self.status = 'pending'
    end
  end

  def set_coin_currency
    self.coin_currency = coin_withdrawal.coin_currency if coin_withdrawal.present?
  end
end
