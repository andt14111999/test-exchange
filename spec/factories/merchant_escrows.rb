# frozen_string_literal: true

FactoryBot.define do
  factory :merchant_escrow do
    association :user, :merchant
    usdt_account { user.coin_accounts.find_by!(coin_currency: 'usdt', layer: 'bep20') }
    fiat_account { user.fiat_accounts.find_by!(currency: 'VND') }
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
  end
end
