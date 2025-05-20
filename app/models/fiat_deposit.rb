# frozen_string_literal: true

class FiatDeposit < ApplicationRecord
  include AASM

  belongs_to :user
  belongs_to :fiat_account
  belongs_to :payable, polymorphic: true, optional: true
  has_one :trade, foreign_key: 'fiat_token_deposit_id', dependent: :nullify

  STATUSES = %w[
    awaiting pending money_sent ready informed verifying
    ownership_verifying locked locked_due_to_unverified_ownership
    processed cancelled illegal refunding refunded
  ].freeze

  validates :currency, presence: true
  validates :country_code, presence: true
  validates :fiat_amount, presence: true, numericality: { greater_than: 0 }
  validates :memo, uniqueness: true, allow_blank: true
  validates :status, presence: true, inclusion: { in: STATUSES }

  # Basic scopes
  scope :unprocessed, -> { where.not(status: %w[processed cancelled refunded illegal]) }
  scope :pending_user_action, -> { where(status: %w[pending money_sent ownership_verifying]) }
  scope :pending_admin_action, -> { where(status: %w[ready informed verifying locked locked_due_to_unverified_ownership]) }
  scope :processing, -> { where(status: %w[pending money_sent reflect_data_pending ready informed verifying ownership_verifying]) }
  scope :processed, -> { where(status: 'processed') }
  scope :cancelled, -> { where(status: 'cancelled') }
  scope :refunding, -> { where(status: %w[refunding refunded]) }
  scope :illegal, -> { where(status: 'illegal') }
  scope :locked, -> { where(status: %w[locked locked_due_to_unverified_ownership]) }

  # Purpose scopes
  scope :for_trade, -> { where(payable_type: 'Trade') }
  scope :direct, -> { where(payable_type: nil) }

  # Filter scopes
  scope :of_currency, ->(currency) { where(currency: currency) }
  scope :of_country, ->(country_code) { where(country_code: country_code) }
  scope :with_explorer_ref, -> { where.not(explorer_ref: nil) }
  scope :without_explorer_ref, -> { where(explorer_ref: nil) }
  scope :needs_ownership_verification, -> { where(status: %w[ownership_verifying locked_due_to_unverified_ownership]) }

  # Time-based scopes
  scope :recent, -> { order(created_at: :desc) }
  scope :timeout_candidates, -> { pending.where('created_at < ?', 7.days.ago) }
  scope :recent_money_sent, -> { where('money_sent_at > ?', 24.hours.ago) }
  scope :needs_verification, -> { where(status: 'ready').where('updated_at < ?', 2.hours.ago) }

  before_create :set_deposit_fee
  before_create :generate_memo, if: -> { memo.blank? }
  after_update :create_transaction_on_process, if: -> { saved_change_to_status? && status == 'processed' }
  after_update :notify_user_on_status_change, if: :saved_change_to_status?

  attr_accessor :cancel_reason_param

  # AASM State Machine
  aasm column: 'status', whiny_transitions: false do
    # Basic flow states
    state :awaiting, initial: true
    state :pending
    state :money_sent
    state :ready
    state :informed
    state :verifying
    state :processed
    state :cancelled

    # Ownership verification flow
    state :ownership_verifying
    state :locked_due_to_unverified_ownership
    state :locked

    # Illegal/refund flow
    state :illegal
    state :refunding
    state :refunded

    # Basic flow transitions
    event :mark_as_pending do
      transitions from: [ :awaiting ], to: :pending
    end

    event :mark_as_money_sent do
      transitions from: [ :pending ], to: :money_sent
    end

    event :mark_as_ready do
      transitions from: [ :awaiting, :pending ], to: :ready
    end

    event :mark_as_informed do
      transitions from: [ :ready ], to: :informed
    end

    event :mark_as_verifying do
      transitions from: [ :ready, :informed, :locked_due_to_unverified_ownership ], to: :verifying
    end

    event :process do
      transitions from: [ :verifying, :ownership_verifying, :ready, :informed ], to: :processed,
                 after: :set_processed_timestamp
    end

    event :cancel do
      transitions from: [
        :awaiting, :pending, :ready,
        :informed, :verifying, :ownership_verifying,
        :locked, :locked_due_to_unverified_ownership
      ], to: :cancelled,
      after: :set_cancelled_timestamp
    end

    # Ownership verification flow
    event :mark_as_ownership_verifying do
      transitions from: [ :ready, :informed, :verifying ], to: :ownership_verifying
    end

    event :mark_as_locked_due_to_unverified_ownership do
      transitions from: [ :ownership_verifying ], to: :locked_due_to_unverified_ownership
    end

    event :mark_as_locked do
      transitions from: [
        :ready, :informed, :verifying,
        :ownership_verifying, :locked_due_to_unverified_ownership
      ], to: :locked,
      after: :set_lock_reason
    end

    # Illegal/refund flow
    event :mark_as_illegal do
      transitions from: [
        :awaiting, :ready, :informed, :verifying,
        :ownership_verifying, :locked, :locked_due_to_unverified_ownership
      ], to: :illegal
    end

    event :mark_as_refunding do
      transitions from: [
        :cancelled, :locked, :locked_due_to_unverified_ownership, :illegal
      ], to: :refunding
    end

    event :mark_as_refunded do
      transitions from: [ :refunding ], to: :refunded
    end
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id fiat_account_id currency country_code
      fiat_amount original_fiat_amount deposit_fee
      explorer_ref memo fiat_deposit_details
      ownership_proof_url sender_name sender_account_number
      payment_proof_url payment_description
      status cancel_reason payable_type payable_id
      processed_at cancelled_at money_sent_at
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user fiat_account payable trade]
  end

  # Status check methods
  STATUSES.each do |s|
    define_method "#{s}?" do
      status == s
    end
  end

  def for_trade?
    payable_type == 'Trade'
  end

  def timeout_check!
    return unless pending?
    return unless created_at < Rails.application.config.timeouts['deposit_pending'].hours.ago

    cancel!('Deposit timed out')
  end

  def verification_timeout_check!
    return unless ready?
    return unless updated_at < Rails.application.config.timeouts['deposit_verification'].hours.ago

    mark_as_ownership_verifying!
  end

  def verify_ownership!(proof_url, sender_name, sender_account)
    update_ownership_proof!(proof_url, sender_name, sender_account)

    if ownership_verified?
      mark_as_verifying! if may_mark_as_verifying?
      true
    else
      mark_as_locked_due_to_unverified_ownership! if may_mark_as_locked_due_to_unverified_ownership?
      false
    end
  end

  def ownership_verified?
    ownership_proof_url.present? &&
      sender_name.present? &&
      sender_account_number.present?
  end

  def perform_timeout_checks!
    if pending? && created_at < Rails.application.config.timeouts['deposit_pending'].hours.ago
      cancel!('Deposit timed out')
    elsif ready? && updated_at < Rails.application.config.timeouts['deposit_verification'].hours.ago
      mark_as_ownership_verifying!
    elsif ownership_verifying? && updated_at < Rails.application.config.timeouts['ownership_verification'].hours.ago
      mark_as_locked_due_to_unverified_ownership!
    end
  end

  def record_bank_response!(response_data)
    current_data = bank_response_data || {}
    update!(bank_response_data: current_data.merge(response_data))
  end

  def increment_verification_attempt!
    increment!(:verification_attempts)
  end

  def max_verification_attempts_reached?
    verification_attempts >= 3
  end

  # Sync deposit status with trade status
  def sync_with_trade_status!
    return unless payable_type == 'Trade' && payable.present?

    trade = payable
    case trade.status
    when 'paid'
      mark_as_ready! if may_mark_as_ready?
    when 'released'
      process! if may_process?
    when 'cancelled', 'cancelled_automatically', 'aborted', 'aborted_fiat'
      cancel!('Trade was cancelled or aborted') if may_cancel?
    when 'disputed'
      mark_as_locked!('Trade is under dispute') if may_mark_as_locked?
    when 'resolved_for_buyer'
      cancel!('Dispute resolved for buyer') if may_cancel?
    when 'resolved_for_seller'
      mark_as_verifying! if may_mark_as_verifying?
    end
  end

  # Transaction Info methods
  def money_sent!
    update!(money_sent_at: Time.zone.now)
    mark_as_money_sent! if may_mark_as_money_sent?
  end

  def record_explorer_ref!(ref)
    update!(explorer_ref: ref)
  end

  def update_ownership_proof!(proof_url, sender_name, sender_account)
    update!(
      ownership_proof_url: proof_url,
      sender_name: sender_name,
      sender_account_number: sender_account
    )
  end

  def amount_after_fee
    fiat_amount - deposit_fee
  end

  private

  def set_processed_timestamp
    self.processed_at = Time.zone.now
  end

  def set_cancelled_timestamp
    self.cancelled_at = Time.zone.now
  end

  def set_lock_reason
    self.cancel_reason = cancel_reason_param if cancel_reason_param.present?
  end

  def set_deposit_fee
    # Calculate deposit fee (can be overridden with custom logic)
    fee_rate = Rails.application.config.deposit_fees[currency] || 0.01
    self.deposit_fee = fiat_amount * fee_rate
  end

  def generate_memo
    self.memo = "Transfer #{SecureRandom.hex(6).upcase}"
  end

  def create_transaction_on_process
    fiat_account.with_lock do
      # Create a transaction record for all deposits
      FiatTransaction.create!(
        fiat_account: fiat_account,
        transaction_type: 'deposit',
        amount: amount_after_fee,
        currency: currency,
        reference: "DEP-#{id}",
        operation: self,
        details: {
          deposit_id: id,
          original_amount: fiat_amount,
          fee: deposit_fee
        }
      )
    end
  end

  def notify_user_on_status_change
    return unless user

    case status
    when 'ready'
      send_notification('Your deposit is ready for verification')
    when 'money_sent'
      send_notification('Your money has been sent')
    when 'ownership_verifying'
      send_notification('Please verify your deposit ownership')
    when 'locked_due_to_unverified_ownership'
      send_notification('Your deposit requires further verification')
    when 'processed'
      send_notification('Your deposit has been successfully processed')
    when 'cancelled'
      send_notification('Your deposit has been cancelled')
    when 'illegal'
      send_notification('Your deposit has been marked as illegal')
    when 'refunding'
      send_notification('Your deposit is being refunded')
    when 'refunded'
      send_notification('Your deposit has been refunded')
    end
  end

  def send_notification(message)
    user.notifications.create!(
      title: "Deposit #{id} Status Update",
      content: message,
      notification_type: 'deposit_status'
    )
  end

  def associate_with_trade
    # Set the trade's fiat_token_deposit_id if not already set
    if payable && payable.is_a?(Trade) && payable.fiat_token_deposit_id.nil?
      payable.update(fiat_token_deposit_id: id)
    end
  end
end
