# frozen_string_literal: true

FactoryBot.define do
  factory :offer do
    association :user
    association :payment_method

    offer_type { %w[buy sell].sample }
    coin_currency { 'btc' }
    currency { 'VND' }
    price { 600_000_000 } # 600M VND per BTC
    min_amount { 0.001 }
    max_amount { 0.1 }
    total_amount { 1.0 }
    payment_time { 30 }
    payment_details { { 'instructions' => 'Please transfer with reference code' } }
    country_code { 'vn' }
    disabled { false }
    deleted { false }
    automatic { false }
    online { true }
    terms_of_trade { 'Please complete the payment within the time limit.' }
    bank_names { %w[Vietcombank BIDV Techcombank] }

    trait :buy do
      offer_type { 'buy' }
    end

    trait :sell do
      offer_type { 'sell' }
    end

    trait :disabled do
      disabled { true }
      disable_reason { 'Temporarily unavailable' }
    end

    trait :deleted do
      deleted { true }
    end

    trait :automatic do
      automatic { true }
    end

    trait :offline do
      online { false }
    end

    trait :with_dynamic_pricing do
      margin { 0.05 } # 5% margin
      fixed_coin_price { nil }
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
