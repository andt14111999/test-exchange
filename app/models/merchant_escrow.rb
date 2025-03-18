# frozen_string_literal: true

class MerchantEscrow < ApplicationRecord
  include AASM

  belongs_to :user
  belongs_to :usdt_account, class_name: 'CoinAccount'
  belongs_to :fiat_account
  has_many :merchant_escrow_operations, dependent: :destroy, inverse_of: :merchant_escrow
  has_one :freeze_operation, -> { where(operation_type: 'freeze') }, dependent: :destroy,
    class_name: 'MerchantEscrowOperation', inverse_of: :merchant_escrow
  has_one :unfreeze_operation, -> { where(operation_type: 'unfreeze') }, dependent: :destroy,
    class_name: 'MerchantEscrowOperation', inverse_of: :merchant_escrow

  validates :usdt_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_currency, presence: true, inclusion: { in: FiatAccount::SUPPORTED_CURRENCIES.keys }
  validates :fiat_amount, presence: true, numericality: { greater_than: 0 }
  validates :status, presence: true

  validate :validate_user_is_merchant
  validate :validate_usdt_account
  validate :validate_fiat_account_currency
  validate :validate_usdt_amount

  before_validation :assign_accounts, on: :create
  after_create :create_freeze_operation

  scope :sorted, -> { order(created_at: :desc) }
  scope :of_currency, ->(currency) { where(fiat_currency: currency) }

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
      transitions from: [ :processing ], to: :completed,
        after: :set_completed_at
    end

    event :fail do
      transitions from: [ :processing ], to: :failed,
        after: :create_unfreeze_operation
    end

    event :cancel do
      transitions from: [ :pending ], to: :cancelled,
        after: :create_unfreeze_operation
    end
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[completed_at created_at fiat_account_id fiat_amount fiat_currency id status updated_at
       usdt_account_id usdt_amount user_id]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[fiat_account freeze_operation merchant_escrow_operations unfreeze_operation usdt_account user]
  end

  private

  def validate_user_is_merchant
    errors.add(:user, :not_merchant) unless user&.role == 'merchant'
  end

  def validate_usdt_account
    return if usdt_account&.coin_type == 'usdt' && usdt_account&.main?

    errors.add(:usdt_account, :invalid)
  end

  def validate_fiat_account_currency
    return if fiat_account&.currency == fiat_currency

    errors.add(:fiat_account, :currency_mismatch)
  end

  def validate_usdt_amount
    return if usdt_amount.nil? || usdt_account.nil?
    return unless usdt_amount > usdt_account.available_balance

    errors.add(:usdt_amount, :exceed_available_balance)
  end

  def assign_accounts
    return if usdt_account.present? && fiat_account.present?

    self.usdt_account = user.coin_accounts.of_coin('usdt').main
    self.fiat_account = user.fiat_accounts.of_currency(fiat_currency).first_or_create!(
      balance: 0,
      frozen_balance: 0
    )
  end

  def create_freeze_operation
    merchant_escrow_operations.create!(
      usdt_account: usdt_account,
      fiat_account: fiat_account,
      usdt_amount: usdt_amount,
      fiat_amount: fiat_amount,
      fiat_currency: fiat_currency,
      operation_type: 'freeze'
    )
  end

  def create_unfreeze_operation
    merchant_escrow_operations.create!(
      usdt_account: usdt_account,
      fiat_account: fiat_account,
      usdt_amount: usdt_amount,
      fiat_amount: fiat_amount,
      fiat_currency: fiat_currency,
      operation_type: 'unfreeze'
    )
  end

  def set_completed_at
    update!(completed_at: Time.current)
  end
end
