# frozen_string_literal: true

FactoryBot.define do
  factory :coin_setting do
    currency { 'usdt' }
    deposit_enabled { true }
    withdraw_enabled { true }
    swap_enabled { true }
    layers do
      [
        {
          'layer' => 'erc20',
          'deposit_enabled' => true,
          'withdraw_enabled' => true,
          'swap_enabled' => true,
          'maintenance' => false
        },
        {
          'layer' => 'bep20',
          'deposit_enabled' => true,
          'withdraw_enabled' => true,
          'swap_enabled' => true,
          'maintenance' => false
        }
      ]
    end

    trait :with_maintenance do
      layers do
        [
          {
            'layer' => 'erc20',
            'deposit_enabled' => false,
            'withdraw_enabled' => false,
            'swap_enabled' => false,
            'maintenance' => true
          }
        ]
      end
    end

    trait :eth do
      currency { 'eth' }
      layers do
        [
          {
            'layer' => 'erc20',
            'deposit_enabled' => true,
            'withdraw_enabled' => true,
            'swap_enabled' => true,
            'maintenance' => false
          }
        ]
      end
    end

    trait :btc do
      currency { 'btc' }
      layers do
        [
          {
            'layer' => 'bitcoin',
            'deposit_enabled' => true,
            'withdraw_enabled' => true,
            'swap_enabled' => true,
            'maintenance' => false
          }
        ]
      end
    end
  end
end
