# frozen_string_literal: true

FactoryBot.define do
  factory :api_key do
    association :user
    sequence(:name) { |n| "API Key #{n}" }

    trait :revoked do
      revoked_at { Time.current }
    end
  end
end
