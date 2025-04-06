# frozen_string_literal: true

class CoinWithdrawalOperation < Operation
  include AASM

  has_many :coin_transactions, as: :operation, dependent: :destroy
  belongs_to :coin_withdrawal, touch: true

  delegate :user, :record_tx_hash_arrived_at, to: :coin_withdrawal
  delegate :coin_address, to: :coin_withdrawal

  after_initialize lambda {
    if new_record?
      self.coin_amount = coin_withdrawal.try(:coin_amount)
      self.coin_fee = coin_withdrawal.try(:coin_fee).to_d
      self.status = 'pending'
    end
  }

  before_validation lambda {
    self.coin_currency = coin_withdrawal.coin_currency if coin_withdrawal.present?
  }, on: :create

  validate :validate_amount

  after_create :create_coin_transactions
  before_update :record_tx_hash_arrived_at, if: -> { tx_hash_changed? && tx_hash.present? }

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
      transitions from: :relaying, to: :relay_crashed
    end

    event :fail do
      transitions from: :relaying, to: :relay_failed
    end

    event :relay do
      transitions from: :relaying, to: :processed,
        guard: :withdrawal_status_processed?,
        after: :mark_withdrawal_release_succeed
    end
  end

  after_commit :relay_later, if: -> { pending? && !Rails.env.test? }, on: :create

  def relay_later
    start_relaying!
    relay_now
  end

  def relay_now
    return unless relaying?

    begin
      params = {
        amount: coin_amount,
        address: coin_address
      }

      process_withdrawal(params)

      if withdrawal_data['status'] == 'processed'
        relay!
      else
        self.status_explanation = 'Withdrawal failed'
        fail!
      end
    rescue StandardError => e
      Rails.logger.error("CoinWithdrawalOperation##{id} relay_now error: #{e.message}")
      crash!
    end
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

  def process_withdrawal(params)
    self.withdrawal_data = {
      'status' => 'processed',
      'tx_hash' => "tx_#{Time.current.to_i}",
      'address' => params[:address],
      'amount' => params[:amount]
    }
    self.withdrawal_status = withdrawal_data['status']
    self.tx_hash = withdrawal_data['tx_hash']
    save!
  end

  def mark_withdrawal_release_processed
    coin_withdrawal.process! if coin_withdrawal.may_process?
  rescue StandardError => e
    Rails.logger.error("CoinWithdrawalOperation##{id} mark_withdrawal_release_processed error: #{e.message}")
  end

  def mark_withdrawal_release_succeed
    return unless withdrawal_status_processed? && tx_hash.present?
    coin_withdrawal.complete! if coin_withdrawal.may_complete?
  rescue StandardError => e
    Rails.logger.error("CoinWithdrawalOperation##{id} mark_withdrawal_release_succeed error: #{e.message}")
  end
end
