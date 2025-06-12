# frozen_string_literal: true

class AmmPool < ApplicationRecord
  include AASM
  include Ransackable

  acts_as_paranoid

  has_many :amm_positions, dependent: :restrict_with_error
  has_many :ticks, dependent: :destroy

  validates :pair, presence: true, uniqueness: true
  validates :token0, presence: true
  validates :token1, presence: true
  validates :tick_spacing, presence: true
  validates :fee_percentage, presence: true
  validates :token0, uniqueness: { scope: :token1, message: 'the pool of token0 and token1 already exists' }
  validates :init_price, numericality: { greater_than: 0 }, allow_nil: true

  UPDATABLE_ATTRIBUTES = %w[fee_percentage fee_protocol_percentage status init_price].freeze

  aasm column: 'status' do
    state :pending, initial: true
    state :active
    state :inactive
    state :failed

    event :activate do
      transitions from: [ :pending, :inactive ], to: :active
    end

    event :deactivate do
      transitions from: [ :active, :pending ], to: :inactive
    end

    event :fail do
      before do |status_explanation|
        self.status_explanation = status_explanation
      end

      transitions from: [ :pending, :active, :inactive ], to: :failed
    end
  end

  after_create :send_event_create_amm_pool
  after_commit :broadcast_update

  def title
    "#{pair} - #{status}"
  end

  def send_tick_query
    payload = {
      eventId: "tick-query-#{SecureRandom.uuid}",
      operationType: KafkaService::Config::OperationTypes::TICK_QUERY,
      actionType: self.class.name,
      actionId: id,
      poolPair: pair
    }

    KafkaService::Services::Tick::TickService.new.query(pool_pair: pair, payload: payload)
  end

  # Tính toán APR (Annual Percentage Rate) của pool
  # Để tính APR chính xác, cần:
  # 1. Thời gian từ khi pool được tạo hoặc từ lần cập nhật phí cuối cùng
  # 2. Tổng phí thu được trong khoảng thời gian đó
  # 3. Tổng giá trị khóa trong pool
  def apr
    return 0 if total_value_locked_token0.to_d.zero? && total_value_locked_token1.to_d.zero?

    # Tính tổng phí thu được
    total_fees = fee_growth_global0.to_d

    # Tính tổng giá trị khóa trong pool theo token0 (USDT)
    total_value_locked = tvl_in_token0

    # Tính APR = (total_fees / total_value_locked) * 100
    # Lưu ý: Đây là ước tính đơn giản, để tính chính xác cần thêm thông tin về thời gian
    (total_fees / total_value_locked * 100).round(2)
  end

  # Tính toán TVL (Total Value Locked) theo token0 (USDT)
  def tvl_in_token0
    # TVL = total_value_locked_token0 + (total_value_locked_token1 / price)
    (total_value_locked_token0.to_d + total_value_locked_token1.to_d / price.to_d).round(2)
  end

  # Tính toán TVL (Total Value Locked) theo token1 (VND)
  def tvl_in_token1
    # TVL = (total_value_locked_token0 * price) + total_value_locked_token1
    (total_value_locked_token0.to_d * price.to_d + total_value_locked_token1.to_d).round(2)
  end

  def send_event_update_amm_pool(params)
    unless validate_update_params(params)
      raise 'No valid changes found in params'
    end

    # Validate init_price updates
    if params[:init_price].present?
      validate_init_price_update(params[:init_price])
    end

    payload = {
      eventId: "amm-pool-#{SecureRandom.uuid}",
      operationType: KafkaService::Config::OperationTypes::AMM_POOL_UPDATE,
      actionType: self.class.name,
      actionId: id,
      pair: pair,
      feePercentage: params[:fee_percentage],
      feeProtocolPercentage: params[:fee_protocol_percentage],
      isActive: params[:status] == 'active',
      initPrice: params[:init_price]
    }.compact

    KafkaService::Services::AmmPool::AmmPoolService.new.update(pair:, payload:)
  end

  private

  def validate_init_price_update(init_price)
    # Validate that init_price is positive
    unless init_price.to_d.positive?
      raise 'Initial price must be positive'
    end

    # Validate that pool has no liquidity
    if total_value_locked_token0.to_d > 0 || total_value_locked_token1.to_d > 0
      raise 'Cannot modify initPrice on pool with liquidity'
    end

    # Validate that pool is not active
    if active?
      raise 'Cannot modify initPrice on active pool'
    end
  end

  def send_event_create_amm_pool
    return unless pending?

    payload = {
      eventId: "amm-pool-#{SecureRandom.uuid}",
      operationType: KafkaService::Config::OperationTypes::AMM_POOL_CREATE,
      actionType: self.class.name,
      actionId: id,
      pair: pair,
      token0: token0.strip.upcase,
      token1: token1.strip.upcase,
      tickSpacing: tick_spacing,
      feePercentage: fee_percentage,
      feeProtocolPercentage: fee_protocol_percentage,
      isActive: false,
      initPrice: init_price
    }.compact

    KafkaService::Services::AmmPool::AmmPoolService.new.create(pair:, payload:)
  rescue => e
    fail!("Failed to notify exchange engine: #{e.message}")
    Rails.logger.error("Failed to notify exchange engine: #{e.message}")
  end

  def validate_update_params(params)
    invalid_keys = params.keys.map(&:to_s) - UPDATABLE_ATTRIBUTES
    return false if invalid_keys.present?

    has_changes = false
    params.each do |key, value|
      next unless UPDATABLE_ATTRIBUTES.include?(key.to_s)
      original_value = self.send(key)

      if value != original_value
        has_changes = true
        break
      end
    end

    has_changes
  end

  def broadcast_update
    AmmPoolBroadcastService.call(self)
  end
end
