# frozen_string_literal: true

class CoinTransaction < ApplicationRecord
  belongs_to :user
  belongs_to :reference, polymorphic: true, optional: true

  REFERENCE_TYPES = %w[deposit withdrawal trade transfer].freeze
  STATUSES = %w[pending processing completed failed cancelled].freeze

  validates :coin_type, presence: true, inclusion: { in: CoinAccount::SUPPORTED_NETWORKS.keys }
  validates :amount, presence: true
  validates :fee, numericality: { greater_than_or_equal_to: 0 }
  validates :status, presence: true, inclusion: { in: STATUSES }
  validates :reference_type, inclusion: { in: REFERENCE_TYPES }, allow_nil: true

  scope :deposits, -> { where(reference_type: 'deposit') }
  scope :withdrawals, -> { where(reference_type: 'withdrawal') }
  scope :trades, -> { where(reference_type: 'trade') }
  scope :transfers, -> { where(reference_type: 'transfer') }

  scope :pending, -> { where(status: 'pending') }
  scope :processing, -> { where(status: 'processing') }
  scope :completed, -> { where(status: 'completed') }
  scope :failed, -> { where(status: 'failed') }
  scope :cancelled, -> { where(status: 'cancelled') }

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id coin_type amount fee status
      reference_type reference_id created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user reference]
  end
end
