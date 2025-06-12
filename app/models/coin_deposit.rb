# frozen_string_literal: true

class CoinDeposit < ApplicationRecord
  include AASM

  acts_as_paranoid

  belongs_to :coin_account, optional: true
  belongs_to :user, optional: true
  has_one :coin_deposit_operation, autosave: false, dependent: :destroy

  before_validation -> { self.user = coin_account&.user }, on: :create
  before_validation :calculate_coin_fee, on: :create

  validates :tx_hash, presence: true
  validates :coin_amount, presence: true, numericality: { greater_than: 0 }
  validates :coin_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :coin_account, presence: true
  validates :out_index, numericality: { less_than_or_equal_to: 0 }, if: -> { coin_currency == 'eth' }
  validates :tx_hash, uniqueness: { scope: %i[out_index coin_currency coin_account_id] }

  delegate :address, :version, to: :coin_account

  scope :sorted, -> { order('id DESC') }

  after_create :verify_deposit

  # State Machine
  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :verified, after_enter: %i[
      set_verified_at
      send_event_deposit_to_kafka
    ]
    state :locked
    state :rejected
    state :forged

    event :forge do
      transitions from: [ :pending ], to: :forged
    end

    event :verify do
      # transitions from: %i[pending rejected], to: :locked do
      # guard do
      #   unsafe_cross_deposit? || fragmented? || exploited? || coin_deposit_restricted?
      # end
      # end

      transitions from: %i[pending rejected], to: :verified do
        # guard do
        #   !unsafe_cross_deposit? && !fragmented? && !coin_deposit_restricted?
        # end
      end
    end

    event :reject do
      transitions from: [ :pending ], to: :rejected
    end

    event :release do
      transitions from: [ :locked ], to: :verified
    end
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id coin_account_id coin_currency coin_amount coin_fee
      tx_hash out_index confirmations_count required_confirmations_count
      status locked_reason last_seen_ip verified_at created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user coin_account coin_deposit_operation]
  end

  private

  def verify_deposit
    verify!
  end

  def calculate_coin_fee
    self.coin_fee = 0
  end

  def set_verified_at
    update(verified_at: Time.current) if verified? && verified_at.nil?
  end

  def send_event_deposit_to_kafka
    account_key = KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
      user_id: user_id,
      account_id: main_coin_account.id
    )

    KafkaService::Services::Coin::CoinDepositService.new.create(
      user_id: user_id,
      coin: coin_account.coin_currency,
      account_key: account_key,
      deposit: self,
      amount: coin_amount
    )
  end

  def main_coin_account
    @main_coin_account ||= user.coin_accounts.of_coin(coin_currency).main
  end

  def deposit_client
    @deposit_client ||= KafkaService::Services::Coin::CoinDepositService.new
  end
end
