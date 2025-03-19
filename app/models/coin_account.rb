# frozen_string_literal: true

# app/models/coin_account.rb
class CoinAccount < ApplicationRecord
  include CategorizedAccount

  belongs_to :user

  SUPPORTED_NETWORKS = {
    'usdt' => %w[erc20 bep20 trc20],
    'eth' => %w[erc20 bep20],
    'bnb' => %w[bep20],
    'btc' => %w[bitcoin bep20]
  }.freeze

  COIN_AND_LAYER_TO_PORTAL_COIN = {
    'usdt' => {
      'erc20' => 'erct',
      'trc20' => 'trct',
      'bep20' => 'brct',
      'solana' => 'srct'
    }
  }.freeze

  PORTAL_COIN_TO_COIN_CURRENCY =
    COIN_AND_LAYER_TO_PORTAL_COIN.each_with_object({}) do |(coin, layer_to_portal_coin), dict|
      layer_to_portal_coin.each_with_object(dict) { |(_layer, portal_coin), acc| acc[portal_coin] = coin }
    end.freeze

  PORTAL_COIN_TO_LAYER =
    COIN_AND_LAYER_TO_PORTAL_COIN.each_with_object({}) do |(_coin, layer_to_portal_coin), dict|
      layer_to_portal_coin.each_with_object(dict) { |(layer, portal_coin), acc| acc[portal_coin] = layer }
    end.freeze

  ACCOUNT_TYPES = %w[main deposit].freeze

  validates :coin_currency, presence: true, inclusion: { in: SUPPORTED_NETWORKS.keys }
  validates :layer, presence: true
  validates :balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :frozen_balance, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :layer, uniqueness: { scope: %i[user_id coin_currency] }
  validates :account_type, presence: true, inclusion: { in: ACCOUNT_TYPES }
  validates :layer, inclusion: { in: lambda { |account|
    account.main? ? [ 'all' ] : SUPPORTED_NETWORKS[account.coin_currency]
  } }, if: -> { coin_currency.present? }
  validate :validate_balances

  scope :of_coin, ->(coin_currency) { where(coin_currency: coin_currency) }

  class << self
    def ransackable_attributes(_auth_object = nil)
      %w[
        id user_id coin_currency layer account_type
        balance frozen_balance
        address created_at updated_at
      ]
    end

    def ransackable_associations(_auth_object = nil)
      %w[user]
    end

    def portal_coin_to_coin_currency(portal_coin)
      PORTAL_COIN_TO_COIN_CURRENCY[portal_coin] || portal_coin
    end

    def portal_coin_to_layer(portal_coin)
      PORTAL_COIN_TO_LAYER[portal_coin]
    end

    def coin_and_layer_to_portal_coin(coin_currency, layer)
      COIN_AND_LAYER_TO_PORTAL_COIN.dig(coin_currency, layer) || coin_currency
    end
  end

  def main?
    account_type == 'main'
  end

  def deposit?
    account_type == 'deposit'
  end

  def available_balance
    balance - frozen_balance
  end

  def handle_deposit(deposit_params)
    out_index = deposit_params[:out_index]
    amount = deposit_params[:amount]
    tx_hash = deposit_params[:tx_hash]
    confirmations_count = deposit_params[:confirmations_count].to_i
    required_confirmations_count = deposit_params[:required_confirmations_count].to_i

    coin = CoinAccount.portal_coin_to_coin_currency(deposit_params[:coin])

    dep = CoinDeposit.where(
      coin_currency: coin,
      tx_hash: tx_hash,
      out_index: out_index,
      coin_amount: amount.to_d.floor(8),
      coin_account: self
    ).first_or_initialize
    dep.confirmations_count = confirmations_count
    dep.required_confirmations_count = required_confirmations_count
    dep.save

    if dep.persisted?
      [ dep, true ]
    else
      [ dep.errors.full_messages.join('; '), false ]
    end
  end

  private

  def validate_balances
    return unless frozen_balance > balance

    errors.add(:frozen_balance, 'cannot be greater than balance')
  end

  def validate_layer_for_coin_currency
    return if coin_currency.blank?
    return if main? && layer == 'all'
    return if deposit? && SUPPORTED_NETWORKS[coin_currency]&.include?(layer)

    errors.add(:layer,
      "is not supported for #{coin_currency}. Supported layers are: #{SUPPORTED_NETWORKS[coin_currency]&.join(', ')}")
  end
end
