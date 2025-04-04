# frozen_string_literal: true

FactoryBot.define do
  factory :coin_account do
    user
    coin_currency { 'usdt' }
    layer { 'erc20' }
    account_type { 'deposit' }
    balance { 0 }
    frozen_balance { 0 }

    trait :with_deposits do
      after(:create) do |account|
        create_list(:coin_deposit, 2, coin_account: account)
      end
    end
  end
end
