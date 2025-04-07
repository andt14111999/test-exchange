# frozen_string_literal: true

FactoryBot.define do
  factory :notification do
    title { 'Test Notification' }
    content { 'This is a test notification' }
    notification_type { 'info' }
    read { false }
    delivered { false }
    association :user

    trait :delivered do
      delivered { true }
    end

    trait :not_delivered do
      delivered { false }
    end
  end
end
