# frozen_string_literal: true

class CoinWithdrawal < ApplicationRecord
  belongs_to :user
  belongs_to :coin_account
  has_one :coin_transaction, as: :reference, dependent: :nullify

  STATUSES = %w[pending processing completed failed cancelled].freeze

  validates :coin_type, presence: true, inclusion: { in: CoinAccount::SUPPORTED_NETWORKS.keys }
  validates :amount, presence: true, numericality: { greater_than: 0 }
  validates :fee, numericality: { greater_than_or_equal_to: 0 }
  validates :blockchain_fee, numericality: { greater_than_or_equal_to: 0 }, allow_nil: true
  validates :destination_address, presence: true
  validates :network, presence: true
  validates :status, presence: true, inclusion: { in: STATUSES }
  validate :validate_network_for_coin_type

  scope :pending, -> { where(status: 'pending') }
  scope :processing, -> { where(status: 'processing') }
  scope :completed, -> { where(status: 'completed') }
  scope :failed, -> { where(status: 'failed') }
  scope :cancelled, -> { where(status: 'cancelled') }

  after_create :create_transaction_record

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id coin_account_id coin_type amount fee blockchain_fee
      destination_address memo network tx_hash reference_id status
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user coin_account coin_transaction]
  end

  private

    def validate_network_for_coin_type
    return if coin_type.blank? || network.blank?
    return if CoinAccount::SUPPORTED_NETWORKS[coin_type]&.include?(network)

    errors.add(:network, "is not supported for #{coin_type}")
    end

  def create_transaction_record
    coin_transaction.create!(
      user: user,
      coin_type: coin_type,
      amount: -amount, # Negative amount for withdrawal
      fee: fee,
      status: status,
      reference_type: 'withdrawal',
      reference_id: id
    )
  end
end
