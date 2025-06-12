# frozen_string_literal: true

FactoryBot.define do
  factory :offer do
    association :user
    association :payment_method

    offer_type { 'buy' }
    coin_currency { 'BTC' }
    currency { 'USD' }
    price { 50000.0 }
    min_amount { 0.001 }
    max_amount { 0.1 }
    total_amount { 1.0 }
    payment_time { 30 }
    country_code { 'US' }
    payment_details { { 'bank_account' => '1234567890' } }
    terms_of_trade { 'Please transfer the exact amount' }
    bank_names { [ 'Bank of America', 'Chase' ] }
    disabled { false }
    deleted { false }
    automatic { false }
    online { true }
    margin { nil }
    schedule_start_time { nil }
    schedule_end_time { nil }

    trait :buy do
      offer_type { 'buy' }
    end

    trait :sell do
      offer_type { 'sell' }
    end

    trait :disabled do
      disabled { true }
      disable_reason { 'Disabled for testing' }
    end

    trait :deleted do
      deleted { true }
      deleted_at { Time.zone.now }
    end

    trait :scheduled do
      schedule_start_time { Time.zone.now - 1.hour }
      schedule_end_time { Time.zone.now + 1.hour }
    end

    trait :with_margin do
      margin { 0.05 }
    end

    trait :automatic do
      automatic { true }
    end

    trait :vietnam do
      currency { 'VND' }
      country_code { 'vn' }
      bank_names { %w[Vietcombank BIDV Techcombank] }
    end

    trait :philippines do
      currency { 'PHP' }
      country_code { 'ph' }
      bank_names { %w[BDO BPI Metrobank] }
    end

    trait :nigeria do
      currency { 'NGN' }
      country_code { 'ng' }
      bank_names { %w[GTBank AccessBank FirstBank] }
    end
  end
end
