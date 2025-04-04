# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_account do
    association :user
    currency { 'VND' }
    balance { 0.0 }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }
  end
end
