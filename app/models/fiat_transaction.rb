# frozen_string_literal: true

class FiatTransaction < ApplicationRecord
  belongs_to :fiat_account
  belongs_to :operation, polymorphic: true

  validates :amount, presence: true, numericality: { other_than: 0 }
  validates :currency, presence: true, inclusion: { in: FiatAccount::SUPPORTED_CURRENCIES.keys }

  delegate :user, to: :fiat_account

  scope :sorted, -> { order(created_at: :desc) }
  scope :of_currency, ->(currency) { where(currency: currency) }

  before_create :take_balance_snapshot

  def self.ransackable_associations(_auth_object = nil)
    %w[fiat_account operation]
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[amount created_at currency fiat_account_id id operation_id operation_type
       snapshot_balance snapshot_frozen_balance transaction_type updated_at]
  end

  private

  def take_balance_snapshot
    self.snapshot_balance = fiat_account.balance + amount
    self.snapshot_frozen_balance = fiat_account.frozen_balance
  end
end
