# frozen_string_literal: true

FactoryBot.define do
  factory :merchant_escrow do
    association :user, factory: :user, role: 'merchant'
    usdt_amount { 100.0 }
    fiat_amount { 100.0 }
    fiat_currency { 'VND' }
    status { 'pending' }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }

    after(:build) do |escrow|
      escrow.usdt_account = escrow.user.coin_accounts.find_by(coin_currency: 'usdt', account_type: 'main')
      escrow.fiat_account = escrow.user.fiat_accounts.find_by(currency: 'VND')
    end
  end
end
