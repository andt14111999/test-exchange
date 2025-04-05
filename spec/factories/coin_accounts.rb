# frozen_string_literal: true

FactoryBot.define do
  factory :coin_account do
    user
    coin_currency { 'usdt' }
    sequence(:layer) { |n| CoinAccount::SUPPORTED_NETWORKS['usdt'][n % CoinAccount::SUPPORTED_NETWORKS['usdt'].length] }
    balance { 0.0 }
    frozen_balance { 0.0 }
    account_type { 'deposit' }
    address { SecureRandom.hex(20) }

    trait :main do
      account_type { 'main' }
      layer { 'all' }
    end

    trait :with_balance do
      balance { 100.0 }
      frozen_balance { 0.0 }
    end

    trait :with_frozen_balance do
      balance { 100.0 }
      frozen_balance { 30.0 }
    end

    trait :eth do
      coin_currency { 'eth' }
      layer { 'erc20' }
    end

    trait :btc do
      coin_currency { 'btc' }
      layer { 'bitcoin' }
    end

    trait :bnb do
      coin_currency { 'bnb' }
      layer { 'bep20' }
    end

    trait :with_deposits do
      after(:create) do |account|
        create_list(:coin_deposit, 2, coin_account: account)
      end
    end
  end
end
