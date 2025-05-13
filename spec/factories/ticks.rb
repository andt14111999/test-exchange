# frozen_string_literal: true

FactoryBot.define do
  factory :tick do
    association :amm_pool
    sequence(:pool_pair) { |n| "USDT/VND_#{n}" }
    sequence(:tick_index) { |n| n * 10 }
    liquidity_gross { BigDecimal('100') }
    liquidity_net { BigDecimal('50') }
    fee_growth_outside0 { BigDecimal('0') }
    fee_growth_outside1 { BigDecimal('0') }
    tick_initialized_timestamp { Time.current.to_i * 1000 }
    initialized { true }
    status { 'active' }
    sequence(:tick_key) { |n| "USDT/VND_#{n}-#{n * 10}" }
    created_at_timestamp { Time.current.to_i * 1000 }
    updated_at_timestamp { Time.current.to_i * 1000 }

    trait :active do
      liquidity_net { BigDecimal('50') }
      status { 'active' }
    end

    trait :inactive do
      liquidity_net { BigDecimal('0') }
      status { 'inactive' }
    end
  end
end
