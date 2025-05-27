# frozen_string_literal: true

FactoryBot.define do
  factory :balance_lock do
    user
    locked_balances { { 'usdt' => '100.0', 'btc' => '0.001' } }
    status { 'pending' }
    reason { 'Administrative lock for security review' }
    locked_at { Time.current }

    trait :pending do
      status { 'pending' }
    end

    trait :locked do
      status { 'locked' }
    end

    trait :releasing do
      status { 'releasing' }
    end

    trait :released do
      status { 'released' }
      unlocked_at { Time.current }
    end

    trait :failed do
      status { 'failed' }
      reason { 'Lock failed: insufficient balance' }
    end

    trait :single_coin do
      locked_balances { { 'usdt' => '50.0' } }
    end

    trait :multiple_coins do
      locked_balances { { 'usdt' => '100.0', 'btc' => '0.001', 'eth' => '0.1' } }
    end
  end
end
