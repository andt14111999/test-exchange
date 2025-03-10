# frozen_string_literal: true

FactoryBot.define do
  factory :social_account do
    user
    sequence(:email) { |n| "social#{n}@example.com" }
    sequence(:provider_user_id) { |n| "user_#{n}" }
    name { 'Social User' }
    provider { %w[google facebook apple].sample }
    access_token { 'access_token' }
    refresh_token { 'refresh_token' }
    token_expires_at { 1.hour.from_now }
    avatar_url { 'https://example.com/social_avatar.jpg' }
    profile_data { { locale: 'en' } }
  end
end
