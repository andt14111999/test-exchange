# frozen_string_literal: true

FactoryBot.define do
  factory :coin_account do
    association :user
    coin_currency { 'usdt' }
    layer { 'trc20' }
    account_type { 'deposit' }
    balance { 0 }
    frozen_balance { 0 }
    address { 'TRX123' }

    trait :main do
      account_type { 'main' }
      layer { 'all' }
    end

    trait :deposit do
      account_type { 'deposit' }
      layer { 'bitcoin' }
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
