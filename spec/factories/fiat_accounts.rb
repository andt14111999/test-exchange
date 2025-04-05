# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_account do
    user
    currency { FiatAccount::SUPPORTED_CURRENCIES.keys.sample }
    balance { 0 }
    frozen_balance { 0 }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }
  end
end
