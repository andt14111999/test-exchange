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
end
