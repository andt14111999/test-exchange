# frozen_string_literal: true

class Tick < ApplicationRecord
  include Ransackable
  include AASM

  acts_as_paranoid

  belongs_to :amm_pool

  validates :pool_pair, presence: true
  validates :tick_index, presence: true
  validates :tick_key, presence: true, uniqueness: true

  before_validation :generate_tick_key, on: :create
  before_save :update_status_based_on_liquidity

  aasm column: 'status' do
    state :inactive, initial: true
    state :active

    event :activate do
      transitions from: :inactive, to: :active
    end

    event :deactivate do
      transitions from: :active, to: :inactive
    end
  end

  scope :active, -> { where(status: 'active') }
  scope :inactive, -> { where(status: 'inactive') }

  def send_tick_query
    payload = {
      eventId: "tick-query-#{SecureRandom.uuid}",
      operationType: KafkaService::Config::OperationTypes::TICK_QUERY,
      actionType: self.class.name,
      actionId: id,
      poolPair: pool_pair
    }

    KafkaService::Services::Tick::TickService.new.query(pool_pair: pool_pair, payload: payload)
  end

  private

  def generate_tick_key
    self.tick_key = "#{pool_pair}-#{tick_index}" if pool_pair.present? && tick_index.present? && tick_key.blank?
  end

  def update_status_based_on_liquidity
    if liquidity_net.to_d.zero?
      deactivate! if may_deactivate?
    else
      activate! if may_activate?
    end
  end
end
