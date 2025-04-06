# frozen_string_literal: true

FactoryBot.define do
  factory :account do
    sequence(:address) { |n| "address_#{n}" }
    account_type { 'main' }
    coin_currency { 'BTC' }
    layer { 'bitcoin' }
    user
  end
end
