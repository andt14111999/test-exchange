# frozen_string_literal: true

class AmmPool < ApplicationRecord
  include AASM
  include Ransackable

  validates :pair, presence: true, uniqueness: true
  validates :token0, presence: true
  validates :token1, presence: true
  validates :tick_spacing, presence: true
  validates :fee_percentage, presence: true
  validates :token0, uniqueness: { scope: :token1, message: 'the pool of token0 and token1 already exists' }

  UPDATABLE_ATTRIBUTES = %w[fee_percentage fee_protocol_percentage status].freeze

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

  def send_event_update_amm_pool(params)
    unless validate_update_params(params)
      raise 'No valid changes found in params'
    end

    payload = {
      eventId: "amm-pool-#{SecureRandom.uuid}",
      operationType: KafkaService::Config::OperationTypes::AMM_POOL_UPDATE,
      actionType: self.class.name,
      actionId: id,
      pair: pair,
      feePercentage: params[:fee_percentage],
      feeProtocolPercentage: params[:fee_protocol_percentage],
      isActive: params[:status] == 'active'
    }.compact

    KafkaService::Services::AmmPool::AmmPoolService.new.update(pair:, payload:)
  end

  private

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
      isActive: false
    }

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
end
