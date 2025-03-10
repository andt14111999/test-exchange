# frozen_string_literal: true

# app/models/coin_account.rb
class CoinAccount < ApplicationRecord
  belongs_to :user

  SUPPORTED_NETWORKS = {
    'USDT' => %w[erc20 bep20 trc20],
    'ETH' => %w[erc20 bep20],
    'BNB' => %w[bep20],
    'BTC' => %w[bitcoin bep20]
  }.freeze

  ACCOUNT_TYPES = %w[main deposit].freeze

  validates :coin_type, presence: true, inclusion: { in: SUPPORTED_NETWORKS.keys }
  validates :layer, presence: true
  validates :balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :frozen_balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :layer, uniqueness: { scope: %i[user_id coin_type] }
  validates :account_type, presence: true, inclusion: { in: ACCOUNT_TYPES }
  validates :layer, inclusion: { in: lambda { |account|
    account.main? ? [ 'all' ] : SUPPORTED_NETWORKS[account.coin_type]
  } }, if: -> { coin_type.present? }
  validate :validate_balances

  scope :of_coin, ->(coin_type) { where(coin_type: coin_type) }
  scope :main, -> { where(account_type: 'main') }
  scope :deposit, -> { where(account_type: 'deposit') }

  class << self
    def ransackable_attributes(_auth_object = nil)
      %w[
        id user_id coin_type layer account_type
        balance frozen_balance
        address created_at updated_at
      ]
    end

    def ransackable_associations(_auth_object = nil)
      %w[user]
    end
  end

  def main?
    account_type == 'main'
  end

  def deposit?
    account_type == 'deposit'
  end

  private

  def validate_balances
    return unless frozen_balance > balance

    errors.add(:frozen_balance, 'cannot be greater than balance')
  end

  def validate_layer_for_coin_type
    return if coin_type.blank?
    return if main? && layer == 'all'
    return if deposit? && SUPPORTED_NETWORKS[coin_type]&.include?(layer)

    errors.add(:layer,
      "is not supported for #{coin_type}. Supported layers are: #{SUPPORTED_NETWORKS[coin_type]&.join(', ')}")
  end
end
