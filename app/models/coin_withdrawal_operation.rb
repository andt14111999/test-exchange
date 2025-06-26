# frozen_string_literal: true

class CoinWithdrawalOperation < Operation
  include AASM

  acts_as_paranoid

  delegate :user, :record_tx_hash_arrived_at, to: :coin_withdrawal
  delegate :coin_address, to: :coin_withdrawal

  scope :sorted, -> { order('id ASC') }

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :relaying
    state :relay_failed
    state :relay_crashed
    state :processed

    event :start_relaying do
      transitions from: %i[pending relay_failed relay_crashed], to: :relaying,
        after: :mark_withdrawal_release_processed
    end

    event :crash do
      before do |message|
        self.status_explanation = message if message.present?
      end

      transitions from: :relaying, to: :relay_crashed
    end

    event :fail do
      transitions from: :relaying, to: :relay_failed
    end

    event :relay do
      transitions from: :relaying, to: :processed
    end
  end

  has_many :coin_transactions, as: :operation, dependent: :destroy
  belongs_to :coin_withdrawal, touch: true

  validate :validate_amount

  after_initialize :set_default_values
  before_validation :set_coin_currency, on: :create
  after_create :create_coin_transactions
  before_update :record_tx_hash_arrived_at, if: -> { tx_hash_changed? && tx_hash.present? }
  after_commit :relay_later, if: -> { pending? && !Rails.env.test? }, on: :create
  after_commit :mark_withdrawal_release_succeed, if: :should_mark_withdrawal_release_succeed?

  def relay_later
    start_relaying!
    relay_now
  end

  def relay_now
    return unless relaying?

    withdrawal_response = send_withdrawal_to_coin_portal
    if withdrawal_response.code < 300
      process_withdrawal!(withdrawal_response.body)
    else
      self.status_explanation = withdrawal_response.body
      fail!
    end
  rescue StandardError => e
    Rails.logger.error("CoinWithdrawalOperation##{id} relay_now error: #{e.message}")
    crash!(e.message)
  end

  def process_withdrawal!(withdrawal)
    self.withdrawal_data = withdrawal
    self.withdrawal_status = withdrawal['status']
    self.tx_hash = withdrawal['tx_hash']
    relay!
  end

  def sync_withdrawal!(withdrawal)
    self.withdrawal_data = withdrawal
    self.tx_hash = withdrawal['tx_hash']
    self.withdrawal_status = withdrawal['status']
    save!
  end

  def send_withdrawal_to_coin_portal
    PostbackService.new(
      target_url: "#{ENV['COIN_PORTAL_URL']}/api/v1/withdrawals/request",
      payload: {
        coin: coin_withdrawal.portal_coin,
        amount: coin_amount,
        address: coin_address,
        payment_id: id
      }
    ).post
  end

  def required_coin_amount
    coin_amount + coin_fee
  end

  def withdrawal_status_processed?
    withdrawal_status == 'processed'
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id
      coin_withdrawal_id
      coin_amount
      coin_fee
      coin_currency
      status
      status_explanation
      withdrawal_status
      tx_hash
      tx_hash_arrived_at
      scheduled_at
      withdrawal_data
      created_at
      updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_withdrawal coin_transactions]
  end

  private

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

  def should_mark_withdrawal_release_succeed?
    withdrawal_status_processed? && tx_hash.present? &&
      (saved_change_to_tx_hash? || saved_change_to_withdrawal_status?)
  end

  def validate_amount
    errors.add(:coin_amount, 'must be greater than 0') unless coin_amount&.positive?
    errors.add(:coin_fee, 'must be greater than or equal to 0') if coin_fee.negative?
  end

  def create_coin_transactions
    coin_transactions.create!(
      amount: -required_coin_amount,
      coin_currency: coin_currency,
      coin_account: coin_withdrawal.coin_account
    )
  end

  def mark_withdrawal_release_processed
    coin_withdrawal.process! if coin_withdrawal.may_process?
  rescue StandardError => e
    Rails.logger.error("CoinWithdrawalOperation##{id} mark_withdrawal_release_processed error: #{e.message}")
  end

  def mark_withdrawal_release_succeed
    return if !withdrawal_status_processed? || !tx_hash.present?

    coin_withdrawal.tx_hash = tx_hash
    coin_withdrawal.save
    coin_withdrawal.send_event_complete_withdrawal_to_kafka
  rescue StandardError => e
    Rails.logger.error("CoinWithdrawalOperation##{id} mark_withdrawal_release_succeed error: #{e.message}")
  end
end
