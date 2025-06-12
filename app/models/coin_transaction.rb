# frozen_string_literal: true

class CoinTransaction < ApplicationRecord
  acts_as_paranoid

  belongs_to :coin_account
  belongs_to :operation, polymorphic: true, optional: true

  validates :amount, presence: true, numericality: { other_than: 0 }
  validates :coin_currency, presence: true, inclusion: { in: CoinAccount::SUPPORTED_NETWORKS.keys }
  validates :transaction_type, presence: true, inclusion: { in: %w[transfer lock unlock] }

  scope :sorted, -> { order(created_at: :desc) }

  # Use these if snapshot_balance and snapshot_frozen_balance are not set explicitly
  before_create :set_balance_snapshot, unless: -> { snapshot_balance.present? && snapshot_frozen_balance.present? }

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      amount coin_account_id coin_currency created_at id operation_id operation_type
      transaction_type updated_at snapshot_balance snapshot_frozen_balance
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_account operation]
  end

  private

  def set_balance_snapshot
    self.snapshot_balance = coin_account.balance
    self.snapshot_frozen_balance = coin_account.frozen_balance
  end
end
