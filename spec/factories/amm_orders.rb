# frozen_string_literal: true

FactoryBot.define do
  factory :amm_order do
    user
    amm_pool
    sequence(:identifier) { |n| "amm_order_#{n}" }
    zero_for_one { [ true, false ].sample }
    amount_specified { rand(1..1000) }
    amount_estimated { rand(0..1000) }
    amount_actual { rand(0..1000) }
    amount_received { rand(0..1000) }
    before_tick_index { rand(-100..0) }
    after_tick_index { rand(1..100) }
    fees { { "token0" => rand(0..1).to_s, "token1" => rand(0..1).to_s } }
    status { 'pending' }
    error_message { '' }
    slippage { 0.01 }

    skip_balance_validation { true }

    trait :pending do
      status { 'pending' }
    end

    trait :processing do
      status { 'processing' }
    end

    trait :success do
      status { 'success' }
    end

    trait :error do
      status { 'error' }
      error_message { 'Test error message' }
    end
  end
end
