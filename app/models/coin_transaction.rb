# frozen_string_literal: true

class CoinTransaction < ApplicationRecord
  belongs_to :coin_account, optional: true
  belongs_to :operation, polymorphic: true, optional: true
  alias_attribute :currency, :coin_currency

  validates :coin_account, presence: true
  validates :operation, presence: true
  validates :amount, presence: true, numericality: { other_than: 0 }, allow_nil: false

  delegate :user, to: :coin_account

  scope :sorted, -> { order(created_at: :desc) }
  scope :of_currency, ->(currency) { where(coin_currency: currency) }

  before_create :take_balance_snapshot

  BALANCE_UPDATING_OPERATIONS = %w[
    CoinDepositOperation
    CoinWithdrawalOperation
  ].freeze

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id coin_account_id coin_currency amount
      snapshot_balance snapshot_frozen_balance
      operation_type operation_id
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_account operation]
  end

  private

  def take_balance_snapshot
    self.snapshot_balance = coin_account.balance + amount
    self.snapshot_frozen_balance = coin_account.frozen_balance
  end
end
