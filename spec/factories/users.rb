# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    sequence(:display_name) { |n| "User #{n}" }
    avatar_url { 'https://example.com/avatar.jpg' }
    role { 'user' }
    status { 'active' }
    kyc_level { 0 }
    phone_verified { false }
    document_verified { false }

    trait :with_default_accounts do
      after(:create) do |user|
        AccountCreationService.new(user).create_all_accounts
      end
    end

    trait :admin do
      role { 'admin' }
    end

    trait :merchant do
      role { 'merchant' }
    end

    trait :suspended do
      status { 'suspended' }
    end

    trait :pending do
      status { 'pending' }
    end

    trait :kyc_level_one do
      kyc_level { 1 }
    end

    trait :kyc_level_two do
      kyc_level { 2 }
    end

    trait :kyc_level_three do
      kyc_level { 3 }
    end

    trait :phone_verified do
      phone_verified_at { Time.current }
    end

    trait :document_verified do
      document_verified_at { Time.current }
    end

    trait :with_2fa do
      authenticator_enabled { true }
      authenticator_key { ROTP::Base32.random_base32 }
    end
  end
end
