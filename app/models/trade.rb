# frozen_string_literal: true

class Trade < ApplicationRecord
  include AASM

  belongs_to :buyer, class_name: 'User'
  belongs_to :seller, class_name: 'User'
  belongs_to :offer
  has_many :messages, dependent: :destroy
  has_one :fiat_deposit, as: :payable, dependent: :nullify
  has_one :fiat_withdrawal, as: :withdrawable, dependent: :nullify

  TAKER_SIDES = %w[buy sell].freeze
  STATUSES = %w[awaiting unpaid paid disputed released cancelled cancelled_automatically].freeze
  PAYMENT_PROOF_STATUSES = %w[legit fake spam].freeze
  DISPUTE_RESOLUTIONS = %w[pending admin_intervention].freeze
  TIMEOUT_MINUTES = 15

  validates :ref, presence: true, uniqueness: true
  validates :buyer_id, presence: true
  validates :seller_id, presence: true
  validates :offer_id, presence: true
  validates :coin_currency, presence: true
  validates :fiat_currency, presence: true
  validates :coin_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_amount, presence: true, numericality: { greater_than: 0 }
  validates :price, presence: true, numericality: { greater_than: 0 }
  validates :fee_ratio, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :fixed_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :coin_trading_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :total_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :amount_after_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :payment_method, presence: true
  validates :taker_side, presence: true, inclusion: { in: TAKER_SIDES }
  validates :status, presence: true, inclusion: { in: STATUSES }
  validates :payment_proof_status, inclusion: { in: PAYMENT_PROOF_STATUSES }, allow_nil: true
  validates :dispute_resolution, inclusion: { in: DISPUTE_RESOLUTIONS }, allow_nil: true
  validates :trade_memo, length: { maximum: 255 }, allow_nil: true

  before_validation :generate_ref, on: :create
  before_validation :generate_trade_memo, on: :create
  before_create :set_initial_timestamps
  before_save :update_status_timestamps
  after_create :send_trade_create_to_kafka
  after_save :create_system_message_on_status_change, if: :saved_change_to_status?
  after_save :update_associated_fiat_deposit, if: :saved_change_to_status?
  after_save :update_associated_fiat_withdrawal, if: :saved_change_to_status?
  after_save :broadcast_trade_update, if: :saved_change_to_status?

  attr_accessor :dispute_reason_param, :admin_notes_param, :is_automatic

  # AASM State Machine
  aasm column: 'status', whiny_transitions: false do
    # Basic states
    state :awaiting, initial: true
    state :unpaid
    state :paid
    state :disputed
    state :released
    state :cancelled
    state :cancelled_automatically

    # Normal trade flow
    event :mark_as_unpaid do
      transitions from: [ :awaiting ], to: :unpaid
    end

    event :mark_as_paid do
      transitions from: [ :unpaid ], to: :paid,
                 after: :set_paid_timestamp
    end

    event :mark_as_disputed do
      transitions from: [ :paid ], to: :disputed,
                 after: :set_dispute_data
    end

    event :mark_as_released do
      transitions from: [ :paid, :disputed ], to: :released,
                 after: :set_released_timestamp
    end

    event :cancel do
      transitions from: [ :awaiting, :unpaid, :paid, :disputed ], to: :cancelled,
                 after: :set_cancelled_timestamp
    end

    event :cancel_automatically do
      transitions from: [ :awaiting, :unpaid ], to: :cancelled_automatically,
                 after: :set_cancelled_timestamp
    end
  end

  # Status scopes
  scope :awaiting, -> { where(status: 'awaiting') }
  scope :unpaid, -> { where(status: 'unpaid') }
  scope :paid, -> { where(status: 'paid') }
  scope :disputed, -> { where(status: 'disputed') }
  scope :released, -> { where(status: 'released') }
  scope :cancelled, -> { where(status: 'cancelled') }
  scope :cancelled_automatically, -> { where(status: 'cancelled_automatically') }
  scope :in_progress, -> { where(status: %w[awaiting unpaid paid disputed]) }
  scope :in_dispute, -> { where(status: 'disputed') }
  scope :needs_admin_intervention, -> { disputed.where('disputed_at < ?', 24.hours.ago) }
  scope :completed, -> { where(status: 'released') }
  scope :with_buyer, ->(user_id) { where(buyer_id: user_id) }
  scope :with_seller, ->(user_id) { where(seller_id: user_id) }
  scope :for_fiat_token, -> { where.not(fiat_token_deposit_id: nil).or(where.not(fiat_token_withdrawal_id: nil)) }
  scope :normal_trades, -> { where(fiat_token_deposit_id: nil, fiat_token_withdrawal_id: nil) }

  # Time scopes
  scope :created_today, -> { where('created_at >= ?', Time.zone.today.beginning_of_day) }
  scope :created_this_week, -> { where('created_at >= ?', Time.zone.today.beginning_of_week) }
  scope :created_this_month, -> { where('created_at >= ?', Time.zone.today.beginning_of_month) }
  scope :expiring_soon, -> { unpaid.where('expired_at > ? AND expired_at < ?', Time.zone.now, 1.hour.from_now) }
  scope :expired, -> { unpaid.where('expired_at < ?', Time.zone.now) }

  # Participant scope
  scope :for_participant, ->(user_id) { where('buyer_id = ? OR seller_id = ?', user_id, user_id) }

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id ref buyer_id seller_id offer_id
      coin_currency fiat_currency coin_amount fiat_amount
      price fee_ratio fixed_fee coin_trading_fee total_fee amount_after_fee
      payment_method taker_side status
      paid_at released_at expired_at cancelled_at disputed_at
      has_payment_proof payment_proof_status dispute_reason dispute_resolution
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[buyer seller offer messages fiat_deposit fiat_withdrawal]
  end

  # Status check methods
  STATUSES.each do |s|
    define_method "#{s}?" do
      status == s
    end
  end

  # Side check methods
  def buyer_is_taker?
    taker_side == 'buy'
  end

  def seller_is_taker?
    taker_side == 'sell'
  end

  # Trade type methods
  def normal_trade?
    fiat_token_deposit_id.nil? && fiat_token_withdrawal_id.nil?
  end

  def fiat_token_trade?
    !normal_trade?
  end

  def is_fiat_token_deposit_trade?
    fiat_token_deposit_id.present?
  end

  def is_fiat_token_withdrawal_trade?
    fiat_token_withdrawal_id.present?
  end

  def fiat_token_deposit
    FiatDeposit.find_by(id: fiat_token_deposit_id)
  end

  def fiat_token_withdrawal
    FiatWithdrawal.find_by(id: fiat_token_withdrawal_id)
  end

  # Time elapsed methods
  def time_since_creation
    Time.zone.now - created_at
  end

  def time_since_payment
    return nil unless paid_at

    Time.zone.now - paid_at
  end

  def time_since_dispute
    return nil unless disputed_at

    Time.zone.now - disputed_at
  end

  def payment_time_left
    return 0 if paid? || released? || cancelled? || disputed?
    return 0 if expired_at && Time.zone.now > expired_at

    [ (expired_at - Time.zone.now).to_i, 0 ].max if expired_at
  end

  def expired?
    expired_at && Time.zone.now > expired_at
  end

  def mark_as_admin_intervention!(admin_notes = nil)
    update!(
      dispute_resolution: 'admin_intervention',
      dispute_resolution_at: Time.zone.now,
      admin_notes: admin_notes
    )
  end

  def needs_admin_intervention?
    disputed? && disputed_at && (Time.zone.now - disputed_at > 24.hours)
  end

  def dispute_expired?
    disputed? && disputed_at && (Time.zone.now - disputed_at > 72.hours)
  end

  def perform_dispute_timeout_check!
    return unless disputed?
    return unless disputed_at < 72.hours.ago

    # If dispute expires, automatically cancel the trade
    cancel!('Automatic cancellation due to dispute timeout')
  end

  def add_payment_proof!(receipt_details)
    update!(
      payment_receipt_details: receipt_details,
      has_payment_proof: true
    )
  end

  def set_payment_proof_status!(status)
    update!(payment_proof_status: status)
  end

  def can_be_cancelled_by?(user)
    # Return false if user is nil
    return false if user.nil?
    return false if released? || cancelled? || cancelled_automatically?
    return true if user.id == buyer_id

    # Seller can cancel only if awaiting or unpaid
    return true if user.id == seller_id && (awaiting? || unpaid?)

    false
  end

  def can_be_disputed_by?(user)
    return false if user.nil?
    return false unless paid?
    user.id == buyer_id || user.id == seller_id
  end

  def can_be_released_by?(user)
    return false if user.nil?
    return false unless paid? || disputed?
    user.id == seller_id
  end

  def may_complete?
    paid? || disputed? || dispute_resolution.present?
  end

  def may_dispute?
    paid? && !released? && !cancelled? && !disputed?
  end

  def may_mark_as_unpaid?
    awaiting? && may_mark_as_unpaid
  end

  def can_be_marked_paid_by?(user)
    return false if user.nil?
    return false unless unpaid?

    # In a normal trade, buyer marks as paid
    if normal_trade?
      return user.id == buyer_id
    end

    # In a deposit trade, admin normally marks as paid
    if is_fiat_token_deposit_trade?
      return user.admin? if defined?(user.admin?)
      return user.id == buyer_id
    end

    # In a withdrawal trade, admin normally marks as paid
    if is_fiat_token_withdrawal_trade?
      return user.admin? if defined?(user.admin?)
      return user.id == buyer_id
    end

    false
  end

  def set_offer_data(offer)
    self.coin_currency = offer.coin_currency
    self.fiat_currency = offer.currency
    self.payment_method = offer.payment_method&.name || 'bank_transfer'
    self.payment_details = offer.payment_details
    self.fee_ratio = Setting.get_trading_fee_ratio(coin_currency)
    self.fixed_fee = Setting.get_fixed_trading_fee(coin_currency)
  end

  def set_price(price_value)
    self.price = price_value
    self.open_coin_price = fetch_current_market_price
  end

  def set_taker_side(taker, side)
    self.taker_side = side

    if side == 'buy'
      self.buyer = taker
      self.seller = offer.user
    else
      self.buyer = offer.user
      self.seller = taker
    end
  end

  def calculate_amounts(amount)
    self.coin_amount = amount
    self.fiat_amount = (amount * price).round(2)
  end

  def calculate_fees
    self.fixed_fee ||= Setting.get_fixed_trading_fee(coin_currency)
    self.fee_ratio ||= Setting.get_trading_fee_ratio(coin_currency)
    self.coin_trading_fee = coin_amount * fee_ratio
    self.total_fee = fixed_fee + coin_trading_fee
    self.amount_after_fee = [ coin_amount - total_fee, 0 ].max
  end

  def fetch_current_market_price
    # This method should be implemented to fetch the current market price
    # from an exchange API or other source
    # For now, we'll just return the trade price
    price
  end

  # Time window management methods
  def payment_window_expired?
    expired_at && Time.zone.now > expired_at
  end

  def extend_payment_window!(minutes = 30)
    update!(expired_at: Time.zone.now + minutes.minutes)
  end

  # Status and flow helpers for fiat token trades
  def start_fiat_token_flow!
    return false unless fiat_token_trade?
    true
  end

  def try_start!
    start_fiat_token_flow! if fiat_token_trade?
    mark_as_unpaid! if awaiting? && may_mark_as_unpaid?
    true
  end

  def process_fiat_token_deposit!
    return false unless is_fiat_token_deposit_trade?
    return false unless fiat_token_deposit&.may_process?

    fiat_token_deposit.process!
  end

  def process_fiat_token_withdrawal!
    return false unless is_fiat_token_withdrawal_trade?
    return false unless fiat_token_withdrawal&.may_process?

    fiat_token_withdrawal.process!
  end

  # Creates fiat withdrawal for the seller
  # Returns true if successful, false otherwise with error_message set
  def create_fiat_withdrawal!
    return [ false, 'Not a sell trade' ] unless seller.present?

    fiat_account = seller.fiat_accounts.find_by(currency: fiat_currency.upcase)
    unless fiat_account
      return [ false, 'You do not have a fiat account for this currency' ]
    end

    withdrawal = FiatWithdrawal.new(
      user_id: seller.id,
      fiat_account_id: fiat_account.id,
      currency: fiat_currency,
      country_code: offer&.country_code,
      fiat_amount: fiat_amount,
      bank_name: payment_details&.dig('bank_name') || offer&.payment_details&.dig('bank_name'),
      bank_account_name: payment_details&.dig('bank_account_name') || offer&.payment_details&.dig('bank_account_name'),
      bank_account_number: payment_details&.dig('bank_account_number') || offer&.payment_details&.dig('bank_account_number'),
      bank_branch: payment_details&.dig('bank_branch') || offer&.payment_details&.dig('bank_branch'),
      withdrawable: self
    )

    if withdrawal.save
      self.fiat_token_withdrawal_id = withdrawal.id
      true
    else
      [ false, "Failed to create fiat withdrawal: #{withdrawal.errors.full_messages.join(', ')}" ]
    end
  end

  # Creates fiat deposit for the buyer
  # Returns true if successful, false otherwise with error_message set
  def create_fiat_deposit!
    return [ false, 'Not a buy trade' ] unless buyer.present?

    fiat_account = buyer.fiat_accounts.find_by(currency: fiat_currency.upcase)
    unless fiat_account
      return [ false, "Buyer does not have a fiat account for this currency #{fiat_currency}" ]
    end

    deposit = FiatDeposit.new(
      user_id: buyer.id,
      fiat_account_id: fiat_account.id,
      currency: fiat_currency,
      country_code: offer&.country_code,
      fiat_amount: fiat_amount,
      payable: self
    )

    if deposit.save
      self.fiat_token_deposit_id = deposit.id
      true
    else
      [ false, "Failed to create fiat deposit: #{deposit.errors.full_messages.join(', ')}" ]
    end
  end

  # Kafka event methods
  def send_trade_create_to_kafka
    trade_service.create(trade: self)
  rescue StandardError => e
    Rails.logger.error("Error sending trade create to Kafka: #{e.message}")
  end

  def send_trade_complete_to_kafka
    trade_service.complete(trade: self)
  rescue StandardError => e
    Rails.logger.error("Error sending trade complete to Kafka: #{e.message}")
  end

  def send_trade_cancel_to_kafka
    trade_service.cancel(trade: self)
  rescue StandardError => e
    Rails.logger.error("Error sending trade cancel to Kafka: #{e.message}")
  end

  def unpaid_timeout?
    return false unless unpaid?
    return false unless paid_at.nil? && Time.zone.now > (created_at + TIMEOUT_MINUTES.minutes)

    true
  end

  def paid_timeout?
    return false unless paid?
    return false unless paid_at && Time.zone.now > (paid_at + TIMEOUT_MINUTES.minutes)

    true
  end

  def perform_timeout_checks!
    if unpaid_timeout?
      send_trade_cancel_to_kafka
      return true
    end

    if paid_timeout?
      self.dispute_reason_param = "System automatic dispute: Trade remained in paid status for over #{TIMEOUT_MINUTES} minutes"
      self.status = 'disputed'
      self.disputed_at = Time.zone.now
      self.dispute_resolution = 'admin_intervention'
      save!
      return true
    end

    false
  end

  private

  def trade_service
    @trade_service ||= KafkaService::Services::Trade::TradeService.new
  end

  def set_paid_timestamp
    self.paid_at = Time.zone.now
  end

  def set_released_timestamp
    self.released_at = Time.zone.now
  end

  def set_cancelled_timestamp
    self.cancelled_at = Time.zone.now
  end

  def set_dispute_data
    self.disputed_at = Time.zone.now
    self.dispute_reason = dispute_reason_param if dispute_reason_param.present?
  end

  def generate_ref
    self.ref ||= "T#{Time.zone.now.strftime('%Y%m%d')}#{SecureRandom.hex(4).upcase}"
  end

  def generate_trade_memo
    return if trade_memo.present?

    # Shorter format: REF part + IDs part + random
    ref_part = ref.last(4).upcase
    buyer_part = buyer_id.to_s.last(2)
    seller_part = seller_id.to_s.last(2)
    random_part = SecureRandom.random_number(100).to_s.rjust(2, '0')

    # Final format: REF-BUYERSELLER-RND
    # Example: ABC1-2345-67
    self.trade_memo = "#{ref_part}-#{buyer_part}#{seller_part}-#{random_part}"
  end

  def set_initial_timestamps
    self.expired_at ||= offer.payment_time.minutes.from_now if offer&.payment_time
  end

  def update_status_timestamps
    Rails.logger.info "Updating status timestamps. Status changed: #{status_changed?}, New status: #{status}"
    self.paid_at = Time.zone.now if status_changed? && status == 'paid' && !paid_at
    self.released_at = Time.zone.now if status_changed? && status == 'released' && !released_at
    self.cancelled_at = Time.zone.now if status_changed? && %w[cancelled cancelled_automatically].include?(status) && !cancelled_at
    self.disputed_at = Time.zone.now if status_changed? && status == 'disputed' && !disputed_at
  end

  def create_system_message_on_status_change
    Rails.logger.info "Creating system message for status change. Status: #{status}"
    message = case status
    when 'unpaid'
                'The trade has been created and is waiting for payment.'
    when 'paid'
                'Buyer has marked the payment as completed.'
    when 'disputed'
                "The trade has been disputed. Reason: #{dispute_reason}"
    when 'released'
                'The coins have been released to buyer.'
    when 'cancelled'
                'The trade has been cancelled.'
    when 'cancelled_automatically'
                'The trade has been automatically cancelled due to timeout.'
    end

    notify_users_of_status_change(message) if message.present?
  end

  def notify_users_of_status_change(message)
    # Create notifications for both users
    [ buyer, seller ].each do |user|
      user.notifications.create!(
        title: "Trade #{ref} Update",
        content: message,
        notification_type: 'trade_status'
      )
    end
  end

  def update_associated_fiat_deposit
    deposit = fiat_deposit || FiatDeposit.find_by(id: fiat_token_deposit_id)

    if deposit.present?
      deposit.sync_with_trade_status!
    end
  end

  def update_associated_fiat_withdrawal
    withdrawal = fiat_withdrawal || FiatWithdrawal.find_by(id: fiat_token_withdrawal_id)

    if withdrawal.present?
      withdrawal.sync_with_trade_status!
    end
  end

  def broadcast_trade_update
    TradeBroadcastService.call(self)
  end
end
