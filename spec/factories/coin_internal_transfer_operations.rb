# frozen_string_literal: true

FactoryBot.define do
  factory :coin_internal_transfer_operation do
    association :coin_withdrawal
    association :sender, factory: :user
    association :receiver, factory: :user
    coin_currency { 'usdt' }
    coin_amount { 10.0 }
    coin_fee { 0.0 }
    status { 'pending' }

    trait :processing do
      after(:create) do |internal_transfer|
        internal_transfer.process!
      end
    end

    trait :completed do
      after(:create) do |internal_transfer|
        internal_transfer.process!
        internal_transfer.complete!
      end
    end

    trait :rejected do
      after(:create) do |internal_transfer|
        internal_transfer.reject!
      end
    end

    trait :canceled do
      after(:create) do |internal_transfer|
        internal_transfer.cancel!
      end
    end
  end
end
