# frozen_string_literal: true

class FiatTransaction < ApplicationRecord
  acts_as_paranoid

  belongs_to :fiat_account
  belongs_to :operation, polymorphic: true, optional: true

  validates :amount, presence: true, numericality: { other_than: 0 }
  validates :transaction_type, presence: true
  validates :currency, presence: true

  TRANSACTION_TYPES = %w[mint burn deposit withdrawal transfer refund lock unlock].freeze
  STATUSES = %w[pending completed failed cancelled].freeze

  validates :transaction_type, inclusion: { in: TRANSACTION_TYPES }
  validates :status, inclusion: { in: STATUSES }, allow_blank: true

  delegate :user, to: :fiat_account

  scope :sorted, -> { order(created_at: :desc) }
  scope :of_currency, ->(currency) { where(currency: currency) }
  scope :of_transaction_type, ->(type) { where(transaction_type: type) }
  scope :deposits, -> { where(transaction_type: 'deposit') }
  scope :withdrawals, -> { where(transaction_type: 'withdrawal') }
  scope :transfers, -> { where(transaction_type: 'transfer') }
  scope :refunds, -> { where(transaction_type: 'refund') }

  # Status scopes
  scope :pending, -> { where(status: 'pending') }
  scope :completed, -> { where(status: 'completed') }
  scope :failed, -> { where(status: 'failed') }
  scope :cancelled, -> { where(status: 'cancelled') }

  before_create :set_balance_snapshot, unless: -> { snapshot_balance.present? && snapshot_frozen_balance.present? }
  before_create :set_default_status

  def self.ransackable_associations(_auth_object = nil)
    %w[fiat_account operation]
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id fiat_account_id amount transaction_type currency
      snapshot_balance snapshot_frozen_balance
      created_at updated_at
    ]
  end

  # Status check methods
  def pending?
    status == 'pending'
  end

  def completed?
    status == 'completed'
  end

  def failed?
    status == 'failed'
  end

  def cancelled?
    status == 'cancelled'
  end

  # Status change methods
  def complete!
    update!(status: 'completed')
  end

  def fail!
    update!(status: 'failed')
  end

  def cancel!
    update!(status: 'cancelled')
  end

  private

  def set_balance_snapshot
    self.snapshot_balance = fiat_account.balance
    self.snapshot_frozen_balance = fiat_account.frozen_balance
  end

  def set_default_status
    self.status = 'pending' if status.blank?
  end
end
