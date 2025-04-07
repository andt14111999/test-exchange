# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_account do
    user
    currency { 'VND' }
    balance { 0 }
    frozen_balance { 0 }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }

    after(:build) do |account|
      if account.user.present? && account.currency.present?
        existing_account = FiatAccount.find_by(
          user_id: account.user_id,
          currency: account.currency
        )

        account.currency = "#{account.currency}_#{Time.current.to_i}" if existing_account.present?
      end
    end

    trait :vnd do
      currency { 'VND' }
    end

    trait :php do
      currency { 'PHP' }
    end

    trait :ngn do
      currency { 'NGN' }
    end

    trait :with_balance do
      balance { 100 }
    end
  end
end
