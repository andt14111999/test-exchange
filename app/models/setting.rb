# frozen_string_literal: true

# RailsSettings Model
class Setting < RailsSettings::Base
  # Store display metadata for fields
  cattr_accessor :display_options

  self.display_options = {}

  # Override field method to support display metadata and validation
  def self.field(key, **opts)
    display = opts.delete(:display)
    validates = opts.delete(:validates)
    super
    display_options[key.to_s] = display if display
    @field_validations ||= {}
    @field_validations[key.to_s] = validates if validates
  end

  def self.field_validations
    @field_validations ||= {}
  end

  scope :exchange_rates do
    field :usdt_to_vnd_rate,
          type: :float,
          default: 25000.0,
          validates: { numericality: { greater_than: 0 } },
          display: { type: :number, step: 'any' }

    field :usdt_to_php_rate,
          type: :float,
          default: 57.0,
          validates: { numericality: { greater_than: 0 } },
          display: { type: :number, step: 'any' }

    field :usdt_to_ngn_rate,
          type: :float,
          default: 450.0,
          validates: { numericality: { greater_than: 0 } },
          display: { type: :number, step: 'any' }
  end

  scope :withdrawal_fees do
    field :usdt_erc20_withdrawal_fee,
          type: :float,
          default: 10,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 100 } },
          display: { type: :number, min: 0, max: 100, step: 'any' }

    field :usdt_bep20_withdrawal_fee,
          type: :float,
          default: 1,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 100 } },
          display: { type: :number, min: 0, max: 100, step: 'any' }

    field :usdt_solana_withdrawal_fee,
          type: :float,
          default: 3,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 100 } },
          display: { type: :number, min: 0, max: 100, step: 'any' }

    field :usdt_trc20_withdrawal_fee,
          type: :float,
          default: 2,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 100 } },
          display: { type: :number, min: 0, max: 100, step: 'any' }
  end

  scope :trading_fees do
    field :vnd_trading_fee_ratio,
          type: :float,
          default: 0.001,
          validates: { numericality: { greater_than_or_equal_to: 0.001, less_than_or_equal_to: 1 } },
          display: { type: :number, min: 0.001, max: 1, step: 0.0001 }

    field :php_trading_fee_ratio,
          type: :float,
          default: 0.001,
          validates: { numericality: { greater_than_or_equal_to: 0.001, less_than_or_equal_to: 1 } },
          display: { type: :number, min: 0.001, max: 1, step: 0.0001 }

    field :ngn_trading_fee_ratio,
          type: :float,
          default: 0.001,
          validates: { numericality: { greater_than_or_equal_to: 0.001, less_than_or_equal_to: 1 } },
          display: { type: :number, min: 0.001, max: 1, step: 0.0001 }

    field :default_trading_fee_ratio,
          type: :float,
          default: 0.001,
          validates: { numericality: { greater_than_or_equal_to: 0.001, less_than_or_equal_to: 1 } },
          display: { type: :number, min: 0.001, max: 1, step: 0.0001 }

    field :vnd_fixed_trading_fee,
          type: :float,
          default: 5000,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 1_000_000 } },
          display: { type: :number, min: 0, max: 999_999, step: 'any' }

    field :php_fixed_trading_fee,
          type: :float,
          default: 10,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 1_000_000 } },
          display: { type: :number, min: 0, max: 999_999, step: 'any' }

    field :ngn_fixed_trading_fee,
          type: :float,
          default: 300,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 1_000_000 } },
          display: { type: :number, min: 0, max: 999_999, step: 'any' }

    field :default_fixed_trading_fee,
          type: :float,
          default: 0,
          validates: { numericality: { greater_than_or_equal_to: 0, less_than: 1_000_000 } },
          display: { type: :number, min: 0, max: 999_999, step: 'any' }
  end

  def self.get_html_input_attributes(key)
    display_opts = display_options[key.to_s]
    return {} unless display_opts

    attributes = { type: 'number' }
    attributes[:min] = display_opts[:min].to_s if display_opts[:min]
    attributes[:max] = display_opts[:max].to_s if display_opts[:max]
    attributes[:step] = display_opts[:step].to_s if display_opts[:step]
    attributes
  end

  def self.validate_setting(key, value)
    validation_rules = field_validations[key.to_s]
    return [] unless validation_rules

    errors = []
    numeric_value = value.to_f

    if validation_rules[:numericality]
      rules = validation_rules[:numericality]

      if rules[:greater_than] && numeric_value <= rules[:greater_than]
        errors << "Value must be greater than #{rules[:greater_than]}"
      end

      if rules[:greater_than_or_equal_to] && numeric_value < rules[:greater_than_or_equal_to]
        if rules[:greater_than_or_equal_to] == 0
          errors << 'Value cannot be negative'
        elsif key.to_s.include?('ratio')
          errors << "Trading fee ratio must be at least #{rules[:greater_than_or_equal_to]} (#{(rules[:greater_than_or_equal_to] * 100).round(1)}%)"
        else
          errors << "Value must be at least #{rules[:greater_than_or_equal_to]}"
        end
      end

      if rules[:less_than] && numeric_value >= rules[:less_than]
        errors << "Value must be less than #{rules[:less_than]}"
      end

      if rules[:less_than_or_equal_to] && numeric_value > rules[:less_than_or_equal_to]
        if key.to_s.include?('ratio')
          errors << "Trading fee ratio must be less than or equal to #{rules[:less_than_or_equal_to]} (#{(rules[:less_than_or_equal_to] * 100).round(2)}%)"
        else
          errors << "Value must be less than or equal to #{rules[:less_than_or_equal_to]}"
        end
      end
    end

    errors
  end

  def self.update_with_validation(key, value)
    validation_errors = validate_setting(key, value)

    if validation_errors.any?
      return { success: false, errors: validation_errors }
    end

    begin
      send("#{key}=", value)
      { success: true, errors: [] }
    rescue => e
      { success: false, errors: [ "Failed to update #{key}: #{e.message}" ] }
    end
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
