# frozen_string_literal: true

class CoinWithdrawal < ApplicationRecord
  include AASM

  belongs_to :user
  has_one :coin_withdrawal_operation, dependent: :destroy
  has_one :coin_transaction, as: :reference, dependent: :nullify

  validates :coin_currency, presence: true
  validates :coin_amount, presence: true, numericality: { greater_than: 0 }
  validates :coin_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :coin_address, presence: true
  validates :coin_layer, presence: true
  validates :status, presence: true

  validate :validate_coin_amount
  validate :validate_coin_address_and_layer

  before_validation :assign_coin_layer
  before_validation :calculate_coin_fee, on: :create
  after_create :create_withdrawal_operation
  after_create :freeze_user_balance

  scope :sorted, -> { order(created_at: :desc) }

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :processing
    state :completed
    state :failed
    state :cancelled

    event :process do
      transitions from: [ :pending ], to: :processing
    end

    event :complete do
      transitions from: [ :processing ], to: :completed
    end

    event :fail do
      transitions from: [ :processing ], to: :failed,
        after: :unfreeze_user_balance
    end

    event :cancel do
      transitions from: [ :pending, :processing ], to: :cancelled,
        after: :unfreeze_user_balance
    end
  end

  def coin_account
    @coin_account ||= user.coin_accounts.of_coin(coin_currency).main
  end

  def record_tx_hash_arrived_at
    update(tx_hash_arrived_at: Time.current)
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id coin_currency coin_amount
      coin_fee coin_address coin_layer status
      tx_hash created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user coin_withdrawal_operation coin_transaction]
  end

  def portal_coin
    CoinAccount.coin_and_layer_to_portal_coin(coin_currency, coin_layer)
  end

  private

  def validate_coin_amount
    return errors.add(:coin_amount, :blank) if coin_amount.nil?

    return unless coin_amount + coin_fee > coin_account.available_balance

      errors.add(:coin_amount, :exceed_available_balance)
  end

  def validate_coin_address_and_layer
    return errors.add(:coin_address, :blank) if coin_address.blank?

    errors.add(:coin_layer, :blank) if coin_layer.blank?
  end

  def assign_coin_layer
    self.coin_layer ||= coin_account&.layer
  end

  def calculate_coin_fee
    self.coin_fee = 0
  end

  def freeze_user_balance
    coin_account.with_lock do
      coin_account.update!(
        frozen_balance: coin_account.frozen_balance + coin_amount + coin_fee
      )
    end
  end

  def unfreeze_user_balance
    coin_account.with_lock do
      coin_account.update!(
        frozen_balance: coin_account.frozen_balance - (coin_amount + coin_fee)
      )
    end
  end

  def create_withdrawal_operation
    create_coin_withdrawal_operation!(
      coin_amount: coin_amount,
      coin_fee: coin_fee,
      coin_currency: coin_currency
    )
  end
end
