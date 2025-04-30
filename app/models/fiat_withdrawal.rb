# frozen_string_literal: true

class FiatWithdrawal < ApplicationRecord
  include AASM

  belongs_to :user
  belongs_to :fiat_account
  belongs_to :withdrawable, polymorphic: true, optional: true
  has_one :trade, foreign_key: 'fiat_token_withdrawal_id', dependent: :nullify

  STATUSES = %w[pending processing processed cancelled bank_pending bank_sent bank_rejected].freeze
  VERIFICATION_STATUSES = %w[unverified verifying verified failed].freeze

  validates :currency, presence: true
  validates :country_code, presence: true
  validates :fiat_amount, presence: true, numericality: { greater_than_or_equal_to: 0.00 }
  validates :bank_name, presence: true
  validates :bank_account_name, presence: true
  validates :bank_account_number, presence: true
  validates :status, presence: true, inclusion: { in: STATUSES }
  validates :verification_status, inclusion: { in: VERIFICATION_STATUSES }, allow_nil: true
  validates :retry_count, numericality: { greater_than_or_equal_to: 0 }
  validate :sufficient_funds, on: :create
  validate :withdrawal_limits, on: :create
  validate :bank_account_verification, on: :create

  # Basic status scopes
  scope :unprocessed, -> { where.not(status: %w[processed cancelled bank_rejected]) }
  scope :in_process, -> { where(status: %w[pending processing bank_pending bank_sent]) }
  scope :processed, -> { where(status: 'processed') }
  scope :bank_pending, -> { where(status: 'bank_pending') }
  scope :bank_sent, -> { where(status: 'bank_sent') }
  scope :bank_rejected, -> { where(status: 'bank_rejected') }
  scope :cancelled, -> { where(status: 'cancelled') }

  # Purpose scopes
  scope :for_trade, -> { where(withdrawable_type: 'Trade') }
  scope :direct, -> { where(withdrawable_type: nil) }

  # Filter scopes
  scope :of_currency, ->(currency) { where(currency: currency) }
  scope :of_country, ->(country_code) { where(country_code: country_code) }
  scope :recent, -> { order(created_at: :desc) }
  scope :with_errors, -> { where.not(error_message: nil) }

  # Time-based scopes
  scope :stuck_in_process, -> { in_process.where('updated_at < ?', 24.hours.ago) }
  scope :recent_failures, -> { bank_rejected.where('updated_at > ?', 24.hours.ago) }
  scope :retry_candidates, -> { bank_rejected.where('retry_count < ?', 3) }
  scope :today, -> { where('created_at >= ?', Time.zone.today.beginning_of_day) }
  scope :this_week, -> { where('created_at >= ?', Time.zone.today.beginning_of_week) }

  before_create :set_withdrawal_fee
  before_create :set_amount_after_transfer_fee
  before_create :lock_funds
  after_update :create_transaction_and_release_funds, if: -> { saved_change_to_status? && status == 'processed' }
  after_update :refund_funds_on_cancel, if: -> { saved_change_to_status? && status == 'cancelled' }
  after_update :notify_user_on_status_change, if: :saved_change_to_status?

  attr_accessor :cancel_reason_param, :error_message_param, :verification_failure_reason

  # AASM State Machine
  aasm column: 'status', whiny_transitions: false do
    # Main states
    state :pending, initial: true
    state :processing
    state :processed
    state :cancelled

    # Bank-specific states
    state :bank_pending
    state :bank_sent
    state :bank_rejected

    # Status transitions
    event :mark_as_processing do
      transitions from: [ :pending, :bank_rejected ], to: :processing
    end

    event :process do
      transitions from: [ :processing, :bank_sent ], to: :processed,
                 after: :set_processed_timestamp
    end

    event :cancel do
      transitions from: [ :pending, :processing, :bank_pending, :bank_rejected ], to: :cancelled,
                 after: :set_cancelled_timestamp
    end

    # Bank-specific transitions
    event :mark_as_bank_pending do
      transitions from: [ :processing ], to: :bank_pending
    end

    event :mark_as_bank_sent do
      transitions from: [ :bank_pending ], to: :bank_sent
    end

    event :mark_as_bank_rejected do
      transitions from: [ :bank_pending, :processing ], to: :bank_rejected,
                 after: :set_rejection_reason
    end

    # Retry flow
    event :retry_processing do
      transitions from: [ :bank_rejected ], to: :processing,
                 guard: :can_be_retried?,
                 after: :increment_retry_count
    end
  end

  # AASM State Machine for Verification
  aasm column: 'verification_status', namespace: :verification, whiny_transitions: false do
    state :unverified, initial: true
    state :verifying
    state :verified
    state :failed

    event :start_verification do
      transitions from: [ :unverified, :failed ], to: :verifying,
                 after: :increment_verification_attempts
    end

    event :verify do
      transitions from: [ :verifying ], to: :verified
    end

    event :fail_verification do
      transitions from: [ :verifying ], to: :failed,
                 after: :set_verification_failure_reason
    end
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id fiat_account_id currency country_code
      fiat_amount fee amount_after_transfer_fee
      bank_name bank_account_name bank_account_number bank_branch
      status retry_count error_message cancel_reason verification_status
      processed_at cancelled_at withdrawable_type withdrawable_id
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user fiat_account withdrawable trade]
  end

  # Status methods for verification
  def verification_aasm
    @verification_aasm ||= aasm(:verification)
  end

  # Verification status methods for state checks
  def unverified?
    verification_aasm.unverified?
  end

  def verification_failed?
    verification_aasm.failed?
  end

  def can_retry_verification?
    verification_failed? && verification_attempts < 3
  end

  def cancel!(reason = nil)
    self.cancel_reason_param = reason
    cancel
  end

  def mark_as_bank_rejected!(error = nil)
    self.error_message_param = error
    mark_as_bank_rejected
  end

  def retry!
    return false unless may_retry_processing?
    retry_processing
    true
  end

  def for_trade?
    withdrawable_type == 'Trade'
  end

  def verify_bank_account!
    start_verification
    # Integration with a bank account verification service would go here
    # For now, we'll simulate a successful verification
    begin
      # Verification logic goes here
      verify
      true
    rescue StandardError => e
      self.verification_failure_reason = e.message
      fail_verification
      false
    end
  end

  def can_be_cancelled?
    may_cancel?
  end

  def can_be_retried?
    bank_rejected? && retry_count < 3
  end

  def exceeds_daily_limit?
    daily_limit = Rails.application.config.withdrawal_daily_limits[currency] || Float::INFINITY

    return false if daily_limit == Float::INFINITY

    total_today = user.fiat_withdrawals
                      .of_currency(currency)
                      .where.not(status: 'cancelled')
                      .today
                      .sum(:fiat_amount)

    (total_today + fiat_amount) > daily_limit
  end

  def exceeds_weekly_limit?
    weekly_limit = Rails.application.config.withdrawal_weekly_limits[currency] || Float::INFINITY

    return false if weekly_limit == Float::INFINITY

    total_week = user.fiat_withdrawals
                     .of_currency(currency)
                     .where.not(status: 'cancelled')
                     .this_week
                     .sum(:fiat_amount)

    (total_week + fiat_amount) > weekly_limit
  end

  # Add bank transaction record methods
  def record_bank_transaction!(reference, transaction_date)
    update!(
      bank_reference: reference,
      bank_transaction_date: transaction_date
    )
  end

  def update_bank_response!(response_data)
    current_data = bank_response_data || {}
    update!(bank_response_data: current_data.merge(response_data))
  end

  private

  def set_processed_timestamp
    self.processed_at = Time.zone.now
  end

  def set_cancelled_timestamp
    self.cancelled_at = Time.zone.now
    self.cancel_reason = cancel_reason_param if cancel_reason_param.present?
  end

  def set_rejection_reason
    self.error_message = error_message_param if error_message_param.present?
  end

  def increment_retry_count
    self.retry_count += 1
  end

  def increment_verification_attempts
    self.verification_attempts = (verification_attempts || 0) + 1
  end

  def set_verification_failure_reason
    self.error_message = verification_failure_reason if verification_failure_reason.present?
  end

  def set_withdrawal_fee
    # Calculate withdrawal fee (can be overridden with custom logic)
    fee_rate = Rails.application.config.withdrawal_fees[currency] || 0.01
    self.fee = fiat_amount * fee_rate
  end

  def set_amount_after_transfer_fee
    self.amount_after_transfer_fee = fiat_amount - fee if fiat_amount && fee
  end

  def lock_funds
    fiat_account.with_lock do
      # Check for sufficient funds
      if fiat_account.balance < fiat_amount
        errors.add(:fiat_amount, "exceeds available balance of #{fiat_account.balance} #{currency}")
        throw :abort
      end

      # Lock funds
      fiat_account.update!(
        balance: fiat_account.balance - fiat_amount,
        locked_amount: fiat_account.locked_amount + fiat_amount
      )
    end
  end

  def create_transaction_and_release_funds
    fiat_account.with_lock do
      # Create a withdrawal transaction
      FiatTransaction.create!(
        fiat_account: fiat_account,
        transaction_type: 'withdrawal',
        amount: -fiat_amount,
        reference: "WD-#{id}",
        details: {
          withdrawal_id: id,
          fee: fee,
          amount_after_fee: amount_after_transfer_fee,
          bank_name: bank_name,
          bank_account_name: bank_account_name,
          bank_account_number: bank_account_number
        }
      )

      # Update the account
      fiat_account.update!(
        locked_amount: fiat_account.locked_amount - fiat_amount
      )

      # Process trade related to withdrawal
      if for_trade? && withdrawable.present?
        trade = withdrawable
        trade.mark_as_released! if trade.may_mark_as_released?
      end
    end
  end

  def refund_funds_on_cancel
    fiat_account.with_lock do
      # Create a refund transaction
      FiatTransaction.create!(
        fiat_account: fiat_account,
        transaction_type: 'withdrawal_refund',
        amount: fiat_amount,
        reference: "WD-REFUND-#{id}",
        details: {
          withdrawal_id: id,
          reason: cancel_reason,
          original_amount: fiat_amount
        }
      )

      # Update the account - return funds to balance
      fiat_account.update!(
        balance: fiat_account.balance + fiat_amount,
        locked_amount: fiat_account.locked_amount - fiat_amount
      )
    end
  end

  def sufficient_funds
    return if fiat_amount.nil?

    if fiat_account && fiat_account.balance < fiat_amount
      errors.add(:fiat_amount, "exceeds available balance of #{fiat_account.balance} #{currency}")
    end
  end

  def withdrawal_limits
    if exceeds_daily_limit?
      daily_limit = Rails.application.config.withdrawal_daily_limits[currency]
      errors.add(:fiat_amount, "exceeds daily withdrawal limit of #{daily_limit} #{currency}")
    end

    if exceeds_weekly_limit?
      weekly_limit = Rails.application.config.withdrawal_weekly_limits[currency]
      errors.add(:fiat_amount, "exceeds weekly withdrawal limit of #{weekly_limit} #{currency}")
    end
  end

  def bank_account_verification
    # This would integrate with a bank account verification service
    # For now, we'll just do basic validation
    return if bank_account_number.present? && bank_name.present? && bank_account_name.present?

    errors.add(:bank_account_number, 'invalid or incomplete bank account information')
  end

  def notify_user_on_status_change
    return unless user

    case status
    when 'processing'
      send_notification('Your withdrawal is being processed')
    when 'bank_pending'
      send_notification('Your withdrawal is pending bank processing')
    when 'bank_sent'
      send_notification('Your withdrawal has been sent to the bank')
    when 'processed'
      send_notification('Your withdrawal has been processed successfully')
    when 'cancelled'
      send_notification('Your withdrawal has been cancelled')
    when 'bank_rejected'
      send_notification('Your withdrawal was rejected by the bank')
    end
  end

  def send_notification(message)
    user.notifications.create!(
      title: "Withdrawal #{id} Status Update",
      content: message,
      notification_type: 'withdrawal_status'
    )
  end
end
