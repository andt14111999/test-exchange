# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_account do
    user
    sequence(:currency) { |n| FiatAccount::SUPPORTED_CURRENCIES.keys[n % FiatAccount::SUPPORTED_CURRENCIES.keys.size] }
    balance { 0 }
    frozen_balance { 0 }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }

    after(:build) do |account|
      # Ensure unique currency for each user
      if FiatAccount.exists?(user_id: account.user_id, currency: account.currency)
        account.currency = FiatAccount::SUPPORTED_CURRENCIES.keys.find { |c| !FiatAccount.exists?(user_id: account.user_id, currency: c) }
      end
    end

    trait :vnd do
      currency { 'VND' }
    end

    trait :php do
      currency { 'PHP' }
    end

    trait :ngn do
      currency { 'NGN' }
    end
  end
end
