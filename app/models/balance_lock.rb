# frozen_string_literal: true

class BalanceLock < ApplicationRecord
  include AASM

  acts_as_paranoid

  belongs_to :user
  has_many :balance_lock_operations, dependent: :destroy
  has_many :coin_transactions, as: :operation, dependent: :destroy

  validates :status, presence: true
  validates :locked_at, presence: true, if: :locked?

  after_create :send_event_balance_lock_to_kafka

  scope :locked, -> { where(status: 'locked') }
  scope :released, -> { where(status: 'released') }

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :locked
    state :releasing
    state :released
    state :failed

    event :mark_as_locked do
      before do
        set_locked_at
      end

      after do
        create_balance_lock_operation
      end

      transitions from: [ :pending ], to: :locked
    end

    event :start_releasing do
      after do
        send_event_balance_unlock_to_kafka
      end

      transitions from: [ :locked ], to: :releasing
    end

    event :release do
      before do
        set_unlocked_at
      end

      after do
        unlock_balances
      end

      transitions from: [ :releasing ], to: :released
    end
  end

  def total_locked_amount_for_coin(coin_currency)
    locked_balances[coin_currency.to_s]&.to_d || 0
  end

  def locked_coins
    locked_balances.keys
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id locked_balances status reason
      locked_at unlocked_at created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user balance_lock_operations coin_transactions]
  end

  private

  def set_locked_at
    self.locked_at ||= Time.current
  end

  def set_unlocked_at
    self.unlocked_at ||= Time.current
  end

  def create_balance_lock_operation
    balance_lock_operations.create!(
      operation_type: 'lock',
      status: 'pending'
    )
  end

  def unlock_balances
    balance_lock_operations.create!(
      operation_type: 'release',
      status: 'pending'
    )

    update!(unlocked_at: Time.current)
  end

  def send_event_balance_lock_to_kafka
    coin_account_keys = user.coin_accounts.map do |coin_account|
      KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
        user_id: user_id,
        account_id: coin_account.id
      )
    end

    fiat_account_keys = user.fiat_accounts.map do |fiat_account|
      KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
        user_id: user_id,
        account_id: fiat_account.id
      )
    end

    account_keys = coin_account_keys + fiat_account_keys

    KafkaService::Services::Coin::BalanceLockService.new.create(
      account_keys: account_keys,
      identifier: id.to_s
    )
  rescue StandardError => e
    Rails.logger.error("Failed to send balance lock event to Kafka: #{e.message}")
  end

  def send_event_balance_unlock_to_kafka
    KafkaService::Services::Coin::BalanceLockService.new.unlock(
      lock_id: engine_lock_id.to_s,
      identifier: id.to_s
    )
  rescue StandardError => e
    Rails.logger.error("Failed to send balance unlock event to Kafka: #{e.message}")
  end
end
