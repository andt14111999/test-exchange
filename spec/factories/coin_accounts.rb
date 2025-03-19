# frozen_string_literal: true

FactoryBot.define do
  factory :coin_account do
    association :user
    sequence(:address) { |n| "address_#{n}" }
    coin_type { 'usdt' }
    layer { 'erc20' }
    account_type { 'deposit' }

    trait :with_deposits do
      after(:create) do |account|
        create_list(:coin_deposit, 2, coin_account: account)
      end
    end
  end
end
