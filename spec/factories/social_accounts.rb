# frozen_string_literal: true

FactoryBot.define do
  factory :social_account do
    user
    provider { 'google' }
    sequence(:provider_user_id) { |n| "user_id#{n}" }
    sequence(:email) { |n| "social#{n}@example.com" }
    sequence(:name) { |n| "Social User #{n}" }
    access_token { 'access_token' }
    refresh_token { 'refresh_token' }
    token_expires_at { 1.hour.from_now }
    avatar_url { 'https://example.com/avatar.jpg' }
    profile_data { { 'data' => 'test' } }

    trait :facebook do
      provider { 'facebook' }
    end

    trait :apple do
      provider { 'apple' }
    end
  end
end
