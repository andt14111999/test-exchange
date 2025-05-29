# frozen_string_literal: true

# RailsSettings Model
class Setting < RailsSettings::Base
  scope :exchange_rates do
    field :usdt_to_vnd_rate, type: :decimal, default: 25000.0
    field :usdt_to_php_rate, type: :decimal, default: 57.0
    field :usdt_to_ngn_rate, type: :decimal, default: 450.0
  end

  scope :withdrawal_fees do
    field :usdt_erc20_withdrawal_fee, type: :decimal, default: 10
    field :usdt_bep20_withdrawal_fee, type: :decimal, default: 1
    field :usdt_solana_withdrawal_fee, type: :decimal, default: 3
    field :usdt_trc20_withdrawal_fee, type: :decimal, default: 2
  end

  scope :trading_fees do
    field :vnd_trading_fee_ratio, type: :decimal, default: 0.001
    field :php_trading_fee_ratio, type: :decimal, default: 0.001
    field :ngn_trading_fee_ratio, type: :decimal, default: 0.001
    field :default_trading_fee_ratio, type: :decimal, default: 0.001

    field :vnd_fixed_trading_fee, type: :decimal, default: 5000
    field :php_fixed_trading_fee, type: :decimal, default: 10
    field :ngn_fixed_trading_fee, type: :decimal, default: 300
    field :default_fixed_trading_fee, type: :decimal, default: 0
  end

  def self.ransackable_attributes(auth_object = nil)
    %w[var value created_at updated_at]
  end

  def self.get_exchange_rate(from_currency, to_currency)
    from_currency = from_currency.to_s.downcase
    to_currency = to_currency.to_s.upcase

    return 1.0 if from_currency.upcase == to_currency

    direct_rate_key = "#{from_currency}_to_#{to_currency.downcase}_rate"
    if Setting.respond_to?(direct_rate_key)
      rate = Setting.send(direct_rate_key)
      return rate if rate.present?
    end

    inverse_rate_key = "#{to_currency.downcase}_to_#{from_currency}_rate"
    if Setting.respond_to?(inverse_rate_key)
      inverse_rate = Setting.send(inverse_rate_key)
      return (1.0 / inverse_rate) if inverse_rate.present?
    end

    raise StandardError, "Exchange rate not found for #{from_currency} to #{to_currency}"
  end

  def self.update_exchange_rate(from_currency, to_currency, new_rate)
    from_currency = from_currency.to_s.downcase
    to_currency = to_currency.to_s.downcase

    key = "#{from_currency}_to_#{to_currency}_rate"

    if Setting.respond_to?("#{key}=")
      Setting.send("#{key}=", new_rate)
      new_rate
    else
      raise StandardError, "Exchange rate setting not defined for #{from_currency} to #{to_currency}"
    end
  end

  def self.get_trading_fee_ratio(currency)
    currency = currency.to_s.downcase
    fee_key = "#{currency}_trading_fee_ratio"

    if Setting.respond_to?(fee_key)
      fee = Setting.send(fee_key)
      return fee if fee.present?
    end

    # Return default if currency-specific fee is not found
    Setting.default_trading_fee_ratio
  end

  def self.get_fixed_trading_fee(currency)
    currency = currency.to_s.downcase
    fee_key = "#{currency}_fixed_trading_fee"

    if Setting.respond_to?(fee_key)
      fee = Setting.send(fee_key)
      return fee if fee.present?
    end

    # Return default if currency-specific fee is not found
    Setting.default_fixed_trading_fee
  end
end
