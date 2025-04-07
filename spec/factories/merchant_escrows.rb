# frozen_string_literal: true

FactoryBot.define do
  factory :merchant_escrow do
    user { create(:user, :merchant) }
    association :usdt_account, factory: [ :coin_account, :usdt_main ]
    association :fiat_account, factory: [ :fiat_account, :vnd ]
    usdt_amount { 100 }
    fiat_amount { 2_500_000 }
    fiat_currency { 'VND' }
    exchange_rate { 25_000 }
    status { 'pending' }

    trait :active do
      status { 'active' }
    end

    trait :cancelled do
      status { 'cancelled' }
    end

    trait :completed do
      status { 'completed' }
    end

    trait :with_includes do
      before(:create) do |escrow|
        escrow.user.coin_accounts.load
        escrow.user.fiat_accounts.load
      end
    end
  end
end
