# frozen_string_literal: true

FactoryBot.define do
  factory :merchant_escrow do
    association :user, :merchant
    usdt_account { create(:coin_account, user: user, coin_currency: 'usdt', layer: 'bep20', account_type: 'deposit') }
    fiat_account { create(:fiat_account, user: user, currency: 'VND') }
    usdt_amount { 100.0 }
    fiat_amount { 2500000.0 }
    fiat_currency { 'VND' }
    exchange_rate { 25000.0 }
    status { 'pending' }

    trait :with_includes do
      before(:create) do |escrow|
        escrow.user.coin_accounts.load
        escrow.user.fiat_accounts.load
      end
    end

    trait :active do
      status { 'active' }
    end

    trait :cancelled do
      status { 'cancelled' }
    end

    trait :completed do
      status { 'completed' }
    end
  end
end
