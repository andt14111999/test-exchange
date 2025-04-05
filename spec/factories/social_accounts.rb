# frozen_string_literal: true

FactoryBot.define do
  factory :social_account do
    association :user
    provider { %w[google facebook apple].sample }
    sequence(:provider_user_id) { |n| "user_#{n}" }
    sequence(:email) { |n| "user#{n}@example.com" }
    name { 'John Doe' }
    avatar_url { 'https://example.com/avatar.jpg' }
    token_expires_at { 1.day.from_now }
  end
end
