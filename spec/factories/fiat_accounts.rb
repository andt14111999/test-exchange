# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_account do
    user
    currency { 'VND' }
    balance { 0 }
    frozen_balance { 0 }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }

    trait :vnd do
      currency { 'VND' }
    end

    trait :php do
      currency { 'PHP' }
    end

    trait :ngn do
      currency { 'NGN' }
    end

    trait :with_balance do
      balance { 100 }
    end
  end
end
