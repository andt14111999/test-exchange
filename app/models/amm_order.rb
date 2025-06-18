# frozen_string_literal: true

class AmmOrder < ApplicationRecord
  include Ransackable
  include AASM

  acts_as_paranoid

  attr_accessor :skip_balance_validation

  belongs_to :user
  belongs_to :amm_pool

  delegate :pair, :token0, :token1, to: :amm_pool, prefix: false, allow_nil: true

  validates :identifier, presence: true, uniqueness: true
  validates :amount_specified, numericality: { other_than: 0 }
  validates :amount_estimated, numericality: { greater_than_or_equal_to: 0 }
  validates :amount_actual, numericality: { greater_than_or_equal_to: 0 }
  validates :amount_received, numericality: { greater_than_or_equal_to: 0 }
  validates :slippage, numericality: { greater_than_or_equal_to: 0 }
  validate :user_has_sufficient_balance, on: :create, unless: -> { skip_balance_validation }

  AMM_ORDER_SWAP = 'amm_order_swap'

  aasm column: 'status' do
    state :pending, initial: true
    state :processing
    state :success
    state :error
    state :transaction_error

    event :process do
      transitions from: :pending, to: :processing
    end

    event :succeed do
      transitions from: :processing, to: :success
    end

    event :fail do
      before do |error_msg|
        self.error_message = error_msg
      end
      transitions from: [ :pending, :processing ], to: :error
    end

    event :transaction_fail do
      before do |error_msg|
        self.error_message = error_msg
      end
      transitions from: [ :pending, :processing, :success ], to: :transaction_error
    end
  end

  after_create :process_order
  after_update :broadcast_amm_order_update, if: :saved_change_to_status?

  def account_key0
    user.main_account(token0)&.account_key
  end

  def account_key1
    user.main_account(token1)&.account_key
  end

  def self.generate_identifier(user_id, pool_pair_value, timestamp = Time.zone.now.to_i)
    "amm_order_#{user_id}_#{pool_pair_value.downcase}_#{timestamp}"
  end

  def generate_identifier
    self.identifier = self.class.generate_identifier(user_id, pair)
  end

  private

  def user_has_sufficient_balance
    if amount_specified > 0
      token_to_check = zero_for_one ? token0 : token1
      required_amount = amount_specified
      token_name = zero_for_one ? 'token0' : 'token1'
    else
      token_to_check = zero_for_one ? token1 : token0
      required_amount = amount_estimated
      token_name = zero_for_one ? 'token1' : 'token0'
    end

    account = user.main_account(token_to_check)
    if !account || account.balance < required_amount
      errors.add(:base, "Không đủ số dư #{token_name} để thực hiện giao dịch")
    end
  end

  def process_order
    process!
    send_event_create_amm_order if processing?
  end

  def send_event_create_amm_order
    begin
      payload = {
        eventId: "amm-order-#{SecureRandom.uuid}",
        operationType: AMM_ORDER_SWAP,
        actionType: self.class.name,
        status: status,
        actionId: id,
        identifier: identifier,
        poolPair: pair,
        ownerAccountKey0: account_key0,
        ownerAccountKey1: account_key1,
        zeroForOne: zero_for_one,
        amountSpecified: amount_specified,
        slippage: slippage
      }.compact

      KafkaService::Services::AmmOrder::AmmOrderService.new.create(identifier:, payload:)
    rescue StandardError => e
      Rails.logger.error("Failed to notify exchange engine about AmmOrder creation: #{e.message}")
      fail(e.message)
    end
  end

  def broadcast_amm_order_update
    begin
      AmmOrderBroadcastService.call(user)
    rescue StandardError => e
      Rails.logger.error("Failed to broadcast AMM order update: #{e.message}")
    end
  end
end
