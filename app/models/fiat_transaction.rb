# frozen_string_literal: true

class FiatTransaction < ApplicationRecord
  belongs_to :fiat_account
  belongs_to :operation, polymorphic: true, optional: true

  validates :amount, presence: true, numericality: { greater_than: 0 }
  validates :transaction_type, presence: true
  validates :currency, presence: true

  TRANSACTION_TYPES = %w[mint burn].freeze

  validates :transaction_type, inclusion: { in: TRANSACTION_TYPES }

  delegate :user, to: :fiat_account

  scope :sorted, -> { order(created_at: :desc) }
  scope :of_currency, ->(currency) { where(currency: currency) }
  scope :of_transaction_type, ->(type) { where(transaction_type: type) }

  before_create :set_balance_snapshot, unless: -> { snapshot_balance.present? && snapshot_frozen_balance.present? }

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

  private

  def set_balance_snapshot
    self.snapshot_balance = fiat_account.balance
    self.snapshot_frozen_balance = fiat_account.frozen_balance
  end
end
