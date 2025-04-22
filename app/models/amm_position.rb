# frozen_string_literal: true

class AmmPosition < ApplicationRecord
  include Ransackable
  include AASM

  belongs_to :user
  belongs_to :amm_pool

  # Delegate để lấy thông tin từ amm_pool
  delegate :pair, :token0, :token1, :tick_spacing, to: :amm_pool, prefix: false, allow_nil: true

  validates :identifier, presence: true, uniqueness: true
  validates :status, presence: true, inclusion: { in: %w[pending open closed error], message: 'must be one of: pending, open, closed, error' }
  validates :liquidity, numericality: { greater_than_or_equal_to: 0 }
  validates :slippage, numericality: { greater_than_or_equal_to: 0 }
  validates :amount0, numericality: { greater_than_or_equal_to: 0 }
  validates :amount1, numericality: { greater_than_or_equal_to: 0 }
  validates :amount0_initial, numericality: { greater_than_or_equal_to: 0 }
  validates :amount1_initial, numericality: { greater_than_or_equal_to: 0 }
  validates :fee_growth_inside0_last, numericality: { greater_than_or_equal_to: 0 }
  validates :fee_growth_inside1_last, numericality: { greater_than_or_equal_to: 0 }
  validates :tokens_owed0, numericality: { greater_than_or_equal_to: 0 }
  validates :tokens_owed1, numericality: { greater_than_or_equal_to: 0 }
  validates :fee_collected0, numericality: { greater_than_or_equal_to: 0 }
  validates :fee_collected1, numericality: { greater_than_or_equal_to: 0 }
  validates :tick_lower_index, numericality: { less_than: :tick_upper_index }
  validate :tick_indices_must_be_multiples_of_tick_spacing
  validate :sufficient_account_balances, on: :create

  # Status constants
  STATUS_PENDING = 'pending'
  STATUS_OPEN = 'open'
  STATUS_CLOSED = 'closed'
  STATUS_ERROR = 'error'

  aasm column: 'status' do
    state :pending, initial: true
    state :open
    state :closed
    state :error

    event :open_position do
      transitions from: :pending, to: :open
    end

    event :close do
      transitions from: :open, to: :closed
    end

    event :fail do
      before do |error_msg|
        self.error_message = error_msg
      end
      transitions from: [ :pending, :open ], to: :error
    end
  end

  after_create :send_event_create_amm_position

  def pool_pair
    pair
  end

  def account_key0
    user.main_account(token0)&.account_key
  end

  def account_key1
    user.main_account(token1)&.account_key
  end

  def self.generate_account_keys(user, amm_pool)
    token0 = amm_pool.token0.downcase
    token1 = amm_pool.token1.downcase

    account0 = user.main_account(token0)
    account1 = user.main_account(token1)

    return nil unless account0 && account1

    [ account0.id.to_s, account1.id.to_s ]
  end

  def self.generate_identifier(user_id, pool_pair, timestamp = Time.now.to_i)
    "amm_position_#{user_id}_#{pool_pair.downcase}_#{timestamp}"
  end

  def generate_identifier
    self.identifier = self.class.generate_identifier(user_id, pool_pair)
  end

  def collect_fee
    raise StandardError, 'Cannot collect fee for a position that is not open' unless open?

    payload = {
      eventId: "amm-position-#{SecureRandom.uuid}",
      operationType: 'amm_position_collect_fee',
      actionType: self.class.name,
      actionId: id,
      identifier: identifier
    }
    KafkaService::Services::AmmPosition::AmmPositionService.new.collect_fee(identifier:, payload:)
  end

  def close_position
    raise StandardError, 'Cannot close a position that is not open' unless open?

    payload = {
      eventId: "amm-position-#{SecureRandom.uuid}",
      operationType: 'amm_position_close',
      actionType: self.class.name,
      actionId: id,
      identifier: identifier
    }
    KafkaService::Services::AmmPosition::AmmPositionService.new.close(identifier:, payload:)
  end

  private

  def tick_indices_must_be_multiples_of_tick_spacing
    return unless amm_pool.present?

    if tick_lower_index % tick_spacing != 0
      errors.add(:tick_lower_index, "must be a multiple of the pool's tick spacing (#{tick_spacing})")
    end

    if tick_upper_index % tick_spacing != 0
      errors.add(:tick_upper_index, "must be a multiple of the pool's tick spacing (#{tick_spacing})")
    end
  end

  def sufficient_account_balances
    return unless user.present? && amm_pool.present? && amount0_initial.present? && amount1_initial.present?

    account0 = user.main_account(token0)
    account1 = user.main_account(token1)

    return unless account0 && account1

    if account0.available_balance < amount0_initial
      errors.add(:amount0_initial, "exceeds available balance in #{token0} account")
    end

    if account1.available_balance < amount1_initial
      errors.add(:amount1_initial, "exceeds available balance in #{token1} account")
    end
  end

  def send_event_create_amm_position
    return unless pending?

    begin
      payload = {
        eventId: "amm-position-#{SecureRandom.uuid}",
        operationType: 'amm_position_create',
        actionType: self.class.name,
        actionId: id,
        identifier: identifier,
        poolPair: pool_pair,
        ownerAccountKey0: account_key0,
        ownerAccountKey1: account_key1,
        tickLowerIndex: tick_lower_index,
        tickUpperIndex: tick_upper_index,
        amount0Initial: amount0_initial,
        amount1Initial: amount1_initial,
        slippage: slippage
      }.compact

      KafkaService::Services::AmmPosition::AmmPositionService.new.create(identifier:, payload:)
    rescue StandardError => e
      Rails.logger.error("Failed to notify exchange engine about AmmPosition creation: #{e.message}")
      fail(e.message)
    end
  end
end
