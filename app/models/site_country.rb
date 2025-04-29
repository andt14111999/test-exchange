# frozen_string_literal: true

class SiteCountry < ApplicationRecord
  validates :country_code, presence: true, uniqueness: true
  validates :name, presence: true
  validates :currency, presence: true
  validates :timezone, presence: true

  scope :enabled, -> { where(enabled: true) }
  scope :disabled, -> { where(enabled: false) }

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id country_code name currency timezone enabled
      min_trade_fiat max_trade_fiat max_total_amount_of_offer_for_fiat_token
      supported_banks supported_payment_methods
      created_at updated_at
    ]
  end

  def payment_methods
    PaymentMethod.where(name: supported_payment_methods).enabled.of_country(country_code)
  end

  def enable!
    update!(enabled: true)
  end

  def disable!
    update!(enabled: false)
  end

  def add_payment_method(method_name)
    return if supported_payment_methods.include?(method_name)

    self.supported_payment_methods = supported_payment_methods + [ method_name ]
    save!
  end

  def remove_payment_method(method_name)
    return unless supported_payment_methods.include?(method_name)

    self.supported_payment_methods = supported_payment_methods - [ method_name ]
    save!
  end

  def add_bank_support(bank_name, details = {})
    new_banks = supported_banks.dup
    new_banks[bank_name] = details
    update!(supported_banks: new_banks)
  end

  def remove_bank_support(bank_name)
    new_banks = supported_banks.dup
    new_banks.delete(bank_name)
    update!(supported_banks: new_banks)
  end

  def supported_bank?(bank_name)
    supported_banks.key?(bank_name)
  end

  def supported_payment_method?(method_name)
    supported_payment_methods.include?(method_name)
  end
end
