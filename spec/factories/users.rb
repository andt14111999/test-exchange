# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    display_name { "User #{SecureRandom.hex(4)}" }
    avatar_url { "https://example.com/avatar.jpg" }
    role { 'user' }
    status { 'active' }
    kyc_level { 0 }
    phone_verified { false }
    document_verified { false }

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
  end
end
